# chatSystem
Chat system software made with Java by using both TCP and UDP sockets
You can acess the internal documentatation throw:
https://sway.office.com/fkxY6yudPA80BnK7?ref=Link

Execution steps:
  1. java -jar directory  #Execute the directory (simulates a Dns service, uses UDP)
  2. java -jar server ip (executes the server, example to deploy in localhost "java -jar server localhost")
  3. java -jar cliente ip (conectes to the server)



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
