**DEPRECATED**

# SecuChat
End to end encrypted messaging platform using Firebase.

SecuChat is chatroom oriented. A user creates a room and then sends invites to usernames that are using the service.
Invite is encrypted with the RSA public key of the recipient and can only be opened with their locally stored private key.
The invite cointains the AES encryption key for the messages that are going to be transmitted in the chatroom. 
Users with the key are the only ones able to see the messages. 
Once they leave the room they cannot join back in again without getting an invite.
