#! /usr/bin/bash

argc=$#

if (( argc != 9 )) 
then
	echo "Usage: $0 <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>"
	exit 1
fi

ver=$1
id=$2
sap=$3
mc_addr=$4
mc_port=$5
mdb_addr=$6
mdb_port=$7
mdr_addr=$8
mdr_port=$9

cd src/bin/classes/java/main
java PeerDriver ${ver} ${id} ${sap} ${mc_addr} ${mc_port} ${mdb_addr} ${mdb_port} ${mdr_addr} ${mdr_port}
