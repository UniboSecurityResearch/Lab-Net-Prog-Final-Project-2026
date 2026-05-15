# Requirements

Start from the provided environment, where an industrial network is composed of 2 hosts with 2 switches and the Asset Administration Shell management infrastructure. The two hosts must be considered a working machine (client) and a plant controller (server) that collects data of the working machines. Client and sever communicates using the modbus protocol.
The goals are

- Encrypt the modbus traffic between the client and server via a P4 program (already provided)
- Count the number of packet encrypted for each modbus function code between 1 and 6. It is necessary to implement a different counter in the P4 program for each function code between 1 and 6.
- Implement a new AAS submodel in the network infrastructure that shows the number of packet encrypted per function code number. The function code number must be selected by the user when invoking the operation from the AAS UI
