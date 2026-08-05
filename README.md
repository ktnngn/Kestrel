# Kestrel — Secure P2P Messaging 

Kestrel is a peer-to-peer instant messaging tool for two users that supports encrypted text, photo, voice memo, and file transfer. 
Messages are encrypted using either a 56-bit (DES) or 128-bit (AES) key, derived from
a shared passphrase via PBKDF2 — the passphrase itself is never sent over
the network or used directly as the encryption key.

## Requirements 

- **Java Development Kit (JDK) 17 or newer** installed on every device you
  plan to run it on
- Both devices on the **same local network** (same WiFi) if testing across
  two separate devices

### Checking if Java is installed 

Open a terminal and run: java -version

If this prints a version number, you're set. If not, install a JDK:
- **Windows/Mac**: download from https://adoptium.net (choose the installer
  for your OS) and run it
- Confirm afterward by running `java -version` again

## Getting the code 
Clone the repository and cd into it:
git clone https://github.com/<your-username>/kestrel.git
cd kestrel

## Building

Both commands compile every `.java` file under `src` into a `bin` folder. Pick the one compatible with your operating system and run the commands in your terminal

**Windows (PowerShell):**
javac -d bin (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object FullName)

**Mac/Linux (Terminal):**
javac -d bin $(find src -name "*.java")



## Running — Option 1: Testing on one device (two terminal/GUI windows) 

Open two terminal windows in the project folder.

**Terminal 1 (listener):**
java -cp bin Main listen

**Terminal 2 (connector):**
java -cp bin Main

Two chat windows will open. If one window seems to be missing, check behind
your other open windows — both processes launch their own separate GUI.

## Running — Option 2: Testing on two separate devices 
**Step 1 — Find the IP address of the device that will listen.**

On the listening device:

- **Windows:** run `ipconfig` in PowerShell and look for "IPv4 Address"
  under your active network adapter (e.g. `192.168.1.42`)
- **Mac:** run `ipconfig getifaddr en0` in Terminal (try `en1` if that
  returns nothing)

**Step 2 — Start the listener** on that device:
java -cp bin Main listen

**Step 3 — Connect from the other device**, using the IP from Step 1:
java -cp bin Main <replace with actual ip>

(replace with the actual IP address you found)

**Step 4 — Allow the firewall prompt.** On the listening device, if Windows
Firewall (or macOS's firewall) asks whether to allow the connection, click
**Allow**, at minimum for private/home networks.

Both devices must be on the same WiFi network for this to work without
additional router configuration.

## Using the app 
- **Send a text message**: type in the input box at the bottom and press
  Enter or click Send
- **Send a photo, voice memo, or file**: click the **+** button next to the
  input box and choose the type from the menu, then pick a file
- **View received photos**: they display inline in the chat
- **Play a received voice memo**: click the "Play voice memo" button on the
  message (works reliably for `.wav` files; other audio formats may not
  play, but can still be saved)
- **Open a received file**: click the "Open" button on the message to
  launch it in your OS's default app for that file type
- **Save any received attachment elsewhere**: click "Save As..." on the
  message to choose a save location
- The bottom pane shows the hex-encoded ciphertext for every message sent,
  so you can see the actual encrypted bytes alongside the plaintext chat

## Key length and passphrase 
The shared passphrase and key length (56-bit / 128-bit) are currently set
in `Main.java`:
```java
String passphrase = <replace with your password>;
boolean use128bit = true; // set to false to use 56-bit (DES) instead
```
Both devices must use the **identical passphrase and key length setting**
to communicate successfully. Edit these values, recompile, and rerun to
switch between 56-bit and 128-bit mode.

## Project structure
src/
Main.java entry point — sets up the connection and launches the GUI
crypto/
KeyManager.java derives DES/AES keys from the shared passphrase (PBKDF2)
CryptoEngine.java encrypts/decrypts messages (DES-CBC / AES-CBC)
protocol/
MessageProtocol.java defines the message framing format (type + length + payload)
network/
PeerConnection.java handles the TCP socket connection between peers
GUI/
ChatGUI.java the chat window (Swing)

## Known limitations
- Both devices must be on the same local network (same WiFi/LAN); the
  app does not support connecting across different networks over the
  public internet without additional router configuration (e.g. port
  forwarding)
- Inline photo preview supports JPEG, PNG, GIF, and BMP; other image
  formats will send and can be saved, but may not preview correctly
- Voice memo playback works reliably only for `.wav` files
- The passphrase and key length are set in code rather than prompted at
  runtime
- If you are using two devices, both devices must have the use128bit boolean set to the same value