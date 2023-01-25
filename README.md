Chat System
Chat system software made with Java using both TCP and UDP sockets. The internal documentation can be accessed through the following link: https://sway.office.com/fkxY6yudPA80BnK7?ref=Link

Execution Steps:
  Execute the directory by running: java -jar directory (this simulates a DNS service and uses UDP).
  Execute the server by running: java -jar server <ip>. Example: java -jar server localhost.
  Connect to the server by running: java -jar cliente <ip>.

  
Available Commands:
  Out of Room:
    nick <your nick>: Set your nickname.
    enter <room>: Enter a room.
    roomList: List all available rooms.
    create <room>: Create a new room.
  In Room:
    send <message>: Send a message to the room.
    send_private <nick> <message>: Send a private message to a specific user.
    makeAdmin <nick>: Make a user an admin.
    removeAdmin <nick>: Remove admin privileges from a user.
    kick <nick>: Kick a user from the room.
    info: Show information about the current room.
    history: View the chat history of the current room.
    Please let me know if you need further help.
