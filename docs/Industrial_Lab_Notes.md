# Industrial Network Programmability
## Goals
- [X] Understand what Kathara is and what problem it solves.
- [X] Understand how a Kathara lab is structured and executed.
- [X] Understand the role of Docker containers inside a network lab.
- [ ] Understand how a programmable **P4 data plane** can be **used to create secure conduits** for industrial communication.
- [ ] Understand how to **parse modbus **protocol **with P4**.
- [ ] Understand how the **Asset Administration Shell (AAS)** can be used to **model both data-plane and control-plane** components as assets

## Reference
- [Modbus](https://www.modbustools.com/modbus.html)
- [Read and Write Registers With the CLI](https://github.com/nsg-ethz/p4-learning/blob/master/examples/read_write_registers_cli/README.md)

## Challenge #8
Start from the provided environment, where an industrial network is composed of 2 hosts with 2 switches and the Asset Administration Shell management infrastructure. The two hosts must be considered a working machine (client) and a plant controller (server) that collects data of the working machines.
Client and sever communicates using the modbus
protocol.

- **The goal** is to **encrypt the modbus traffic between the client and server** via a P4 program (already provided) and **count the number of packet encrypted for each modbus function code** between 1 and 6.
- It is necessary to **implement a different counter** in the P4 program for each function code between 1 and 6.
- **Implement a new AAS submodel** in the network infrastructure that **shows the number of packet encrypted per function code** number.
- The function code number must be selected by the user when invoking the operation from the AAS UI.
  
## Lab 1 —> P4 data plane used to create secure conduits
**GOAL:** make *secure conduits* in industrial networks *using symmetric encryption*
### Ingress

Definita la struttura tabella per modbus:

- **tipo match pacchetto:** `exact` (esatto)
- **criterio di match:** `ipv4.dstAddr`
- **azioni disponibili**: cifra, non cifrare, decifra.
- **azione default**: non cifrare

``` c
table modbus_sec {
        key = {
            hdr.ipv4.dstAddr: exact;
        }
        actions = {
            no_cipher;
            cipher;
            decipher;
        }
        size = 2;
        default_action = no_cipher();
    }

    apply {
        if (hdr.ipv4.isValid()){
            ipv4_lpm.apply();
            if (hdr.tcp.isValid()){
                if (hdr.modbus_tcp.isValid()){
                    modbus_sec.apply();
                }
            }
        }
    }
```
Nel caso del progetto oltre verificare che sono validi gli header bisogna (vedi lab 6 di p4):

- creare 6 registri/contatori per ciascun function code (in *ingress*)

Come definire, leggere e scrivere un registro (simil vettore/array):

```c
// dichiarazione di registro con 1024 elementi
// ciasucn elemento è grande 16t bit
register<bit<16>>(1024) reg;

// scrittura
reg.write(idx, value);

//lettura
reg.read(idx);
 ```
- leggere `function code` dall’header modbus
DUBBIO dove fare sta cosa (riferimento file **LAB1_P4_MODBUS** file `industrial_aes.p4`):
  - in ingress quando cifro/decifro (linea 262)
  - oppure in apply di egress dove calcolo poi l’incremento (linea 360)
- in base a function code incrementare il contatore corrispondente

## Lab 2 —> Implement a new AAS submodel
**GOAL:** **Implement a new AAS submodel** in the network infrastructure that **shows the number of packet encrypted per function code** number.
The function code number must be selected by the user when invoking the operation from the AAS UI.

### TODO
- modificare progetto Java **AAS_APP**
- 

Implementare le operation nel file `aas_app/app/src/main/java/p4_aas/Submodels/SwitchRuntime/SwitchRuntimeSubmodel.java`
Qui definisco i campi che vengono visualizzati nella GUI web


```java

@Override
    public List<Submodel> createSubmodel() {
        Submodel switchRuntime = new Submodel();
        switchRuntime.setIdShort("SwitchRuntime");

        switchRuntime.addSubmodelElement(showTables());
        switchRuntime.addSubmodelElement(dumpTable());
        switchRuntime.addSubmodelElement(readRegister());
  
        /* TODO: aggiungere allo switch il submodelEelement(...)
        creato per lettura contatori */

        return List.of(switchRuntime);
    }
```

Prendendo ispirazione da questa Operation scriverne una che ha come:
- **input** —> fornisci la function code (il nome del registro potremmo metterlo hard coded nel codice es. `f_code_counter`)
- **output** —> numero contatore
  
```java
    private Operation readRegister() {
        Operation readRegister = new Operation("ReadRegister");
        Map<String, ValueType> inputVariables = new LinkedHashMap<>();
        inputVariables.put("Switch", ValueType.Integer);
        inputVariables.put("Register", ValueType.String);
        readRegister.setInputVariables(getUtils().getCustomInputVariables(inputVariables));
        readRegister.setOutputVariables(getUtils().getOperationVariables(1, "Output"));
        readRegister.setWrappedInvokable(lambdaProvider.readRegister());
        return readRegister;
    }
```