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

```sh
cd bin
javac ../src/*.java ../src/sdis/*.java ../src/sdis/*/*.java -cp ../src -d .
```

## Run

```sh
cd build
java PeerDriver VERSION PEER_ID SERVICE_ACCESS_POINT MC MC_PORT MDB MDB_PORT MDR MDR_PORT
```

Call `java PeerDriver` for more information on the meaning of each argument.
