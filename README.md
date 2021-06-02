# SDIS Project 2 - Distributed Backup Service for Internet

- **Project name:** Distributed Backup Service for Internet
- **Short description:** Peer-to-peer distributed backup service, using chord
- **Environment:** Unix/Windows console
- **Tools:** Java, RMI
- **Institution:** [FEUP](https://sigarra.up.pt/feup/en/web_page.Inicial)
- **Course:** [SDIS](https://sigarra.up.pt/feup/en/UCURR_GERAL.FICHA_UC_VIEW?pv_ocorrencia_id=459489) (Distributed Systems)
<!-- - **Project grade:** ??/20.0 -->
- **Group:** g21
- **Group members:**
    - [Breno Accioly de Barros Pimentel](https://github.com/BrenoAccioly) (up201800170@edu.fe.up.pt)
    - [Diogo Miguel Ferreira Rodrigues](https://github.com/dmfrodrigues) (up201806429@edu.fe.up.pt)
    - [João António Cardoso Vieira e Basto de Sousa](https://github.com/JoaoASousa) (up201806613@edu.fe.up.pt)
    - [Rafael Soares Ribeiro](https://github.com/up201806330) (up201806330@edu.fe.up.pt)

## Compile

### Linux

```sh
./gradlew assemble
```

### Windows

```cmd
gradlew.bat assemble
```

## Run

Call without arguments for more information on the meaning of each argument.

IPs can also be domain names (usually `localhost`).

### Linux

```sh
# Start rmiregistry
cd build/classes/java/main
rmiregistry

# Run project
cd build/classes/java/main
java PeerDriver PEER_ID SERVICE_ACCESS_POINT PEER_IP [IP:PORT]
```

Example:

```sh
java PeerDriver 1 peer1 localhost
```
