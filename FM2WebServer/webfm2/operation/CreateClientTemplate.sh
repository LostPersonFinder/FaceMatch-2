# create a FM2 Client named <Client>


echo  --In createClient<Client>.sh -- Adding client <Client> 
CMD_DIR=/home/dmisra/FaceMatch2/Operations/commands
echo $CMD_DIR

$CMD_DIR/addClientLinux.sh <Client> _<Client>_ClientInfo.json
#
