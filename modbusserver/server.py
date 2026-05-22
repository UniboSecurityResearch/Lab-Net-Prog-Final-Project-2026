import asyncio

from pymodbus.datastore import ModbusSequentialDataBlock, ModbusServerContext
try:
    from pymodbus.datastore import ModbusSlaveContext
except ImportError:
    from pymodbus.datastore import ModbusDeviceContext as ModbusSlaveContext
from pymodbus.server import StartAsyncTcpServer


DATASTORE_SIZE = 100


def create_slave_context():
    print("[DEBUG] Creating slave context with datastore size:", DATASTORE_SIZE)
    coils = [False] * DATASTORE_SIZE
    coils[0] = True
    discrete_inputs = [(index % 2) == 0 for index in range(DATASTORE_SIZE)]
    holding_registers = [0] * DATASTORE_SIZE
    holding_registers[0] = 43
    input_registers = [1000 + index for index in range(DATASTORE_SIZE)]

    print(f"[DEBUG] Initialized data blocks - coils: {len(coils)}, DI: {len(discrete_inputs)}, HR: {len(holding_registers)}, IR: {len(input_registers)}")

    datastore = {
        "di": ModbusSequentialDataBlock(0, discrete_inputs),
        "co": ModbusSequentialDataBlock(0, coils),
        "hr": ModbusSequentialDataBlock(0, holding_registers),
        "ir": ModbusSequentialDataBlock(0, input_registers),
    }

    print("[DEBUG] Creating ModbusSlaveContext...")
    try:
        context = ModbusSlaveContext(**datastore, zero_mode=True)
        print("[DEBUG] Successfully created ModbusSlaveContext with zero_mode=True")
        return context
    except TypeError:
        print("[DEBUG] zero_mode not supported, creating ModbusSlaveContext without it")
        return ModbusSlaveContext(**datastore)


def create_server_context():
    print("[DEBUG] Creating server context...")
    slave_context = create_slave_context()
    print("[DEBUG] Slave context created, wrapping in ModbusServerContext...")
    try:
        context = ModbusServerContext(slaves=slave_context, single=True)
        print("[DEBUG] Successfully created ModbusServerContext with slaves parameter")
        return context
    except TypeError:
        print("[DEBUG] slaves parameter not supported, using devices parameter")
        return ModbusServerContext(devices=slave_context, single=True)


async def run_async_server():
    print("[DEBUG] Initializing async server...")
    context = create_server_context()
    address = ("200.1.1.7", 502)
    print(f'[DEBUG] Starting Modbus TCP server on {address[0]}:{address[1]} with FC1-FC6 datastore...')
    await StartAsyncTcpServer(context=context, address=address)
    print("[DEBUG] Modbus TCP server started successfully")


async def main():
    print("[DEBUG] Main entry point called")
    await run_async_server()


if __name__ == "__main__":
    print("[DEBUG] Starting Modbus server with debug mode enabled")
    asyncio.run(main(), debug=True)
