# chatSystem
A chat system software made with Java by using both TCP and UDP sockets
You can acess the internal documentatation throw:
https://sway.office.com/fkxY6yudPA80BnK7?ref=Link

Execute steps:
  1. java -jar directory  #Execute the directory (simulates a Dns service, uses UDP)
  2. java -jar server localhost (executes the server in localhost)
  3. java -jar cliente ip (conectes to the server, in this case, if you chose "localhost": java -jar cliente localhost)



Available commands:
(out of room)
nick <your nick>
enter <room>
roomList 
create <room>
  
 ---
 (in room)
 send <message>
 send_private <nick> <message>
 makeAdmin <nick>
 removeAdmin <nick>
 kick <nick> 
 info 
 history
