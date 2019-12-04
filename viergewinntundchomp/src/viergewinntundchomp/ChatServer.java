import java.net.*;
import java.io.*;

public class ChatServer implements Runnable
{
    private ChatServerThread clients[] = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread thread = null;
    private int clientCount = 0;

    public static void main(String args[])
    {
        ChatServer server = new ChatServer(5555);
    }
    public ChatServer(int port)
    {
        try
        {
            server = new ServerSocket(port);
            System.out.println("Server gestartet: " + server);
            start();
        }
        catch(IOException e)
        {
            System.out.println("Ungültiger Port " + port + ": " + e.getMessage()); }
        }
    public void run()
    {
        while (thread != null)
        {
            try
            {
                System.out.println("Wartet auf Clients");
                AddThread(server.accept());
            }
            catch(IOException e)
            {
                System.out.println("Server accept error: " + e);
                stop();
            }
        }
    }
    public void start()
    {
        if (thread == null)
        {
            thread = new Thread(this);
            thread.start();
        }
    }
    public void stop()
    {
        if (thread != null)
        {
            thread = null;
        }
    }
    private int FindClient(int ID)
    {
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].getID() == ID)
            {
                return i;
            }
        }
        return -1;
    }
    public void SendJoined(int ID)
    {
        ChatServerThread client = clients[FindClient(ID)];
        for (int i = 0; i < clientCount; i++)
        {
            if (clients[i].accepted)
            {
                clients[i].Send(client.name + " joined");
            }

        }
        client.Send("Zurzeit im Raum: " + GetList());
    }
    public synchronized void SendMessage(int ID, String input)
    {
        ChatServerThread client = clients[FindClient(ID)];
        if (input.equals("getList") || input.equals("getlist"))
        {
            client.Send(GetList());
        }
        else
        {
            String message = client.name + ": " + input;
            for (int i = 0; i < clientCount; i++)
            {
                if (clients[i].accepted)
                {
                    clients[i].Send(message);
                }
                if (input.equals(".bye"))
                {
                    clients[i].Send(client.name + " disconnected");
                }
            }
            if (input.equals(".bye"))
            {
                Remove(ID);
            }
        }
    }
    public synchronized void Remove(int ID)
    {
        int pos = FindClient(ID);
        if (pos >= 0)
        {
            ChatServerThread toTerminate = clients[pos];
            System.out.println("Client Thread " + ID + " auf Platz " + pos + " wird entfernt");
            if (pos < clientCount - 1)
            {
                for (int i = pos+1; i < clientCount; i++)
                {
                    clients[i-1] = clients[i];
                }
            }
            clientCount--;
            try
            {
                toTerminate.close();
            }
            catch(IOException e)
            {
                System.out.println("Error closing thread: " + e);
            }
        }
    }
    private void AddThread(Socket socket)
    {
        if (clientCount < clients.length)
        {
            System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            try
            {
                clients[clientCount].open();
                clients[clientCount].start();
                clientCount++;
            }
            catch(IOException e)
            {
                System.out.println("Error opening thread: " + e);
            }
        }
        else
        {
            System.out.println("Maximale Anzahl von " + clients.length + " Clients erreicht.");
        }
    }
    public String GetList()
    {
        String namen = "]";
        for (int i = clientCount - 1; i >= 0; i--)
        {
            if (clients[i].name != null)
            {
                if (i > 0)
                {
                    namen = ", " + clients[i].name + namen;
                }
                else
                {
                    namen = clients[i].name + namen;
                }
            }
        }
        namen ="[" + namen;
        return namen;
    }
}

class ChatServerThread extends Thread
{
    private ChatServer server = null;
    private Socket socket = null;
    private int ID = -1;
    private DataInputStream streamIn = null;
    private DataOutputStream streamOut = null;
    public boolean accepted = false;
    public String name;

    public ChatServerThread(ChatServer server, Socket socket)
    {
        super();
        this.server = server;
        this.socket = socket;
        ID = socket.getPort();
    }
    public void Send(String msg)
    {
        try
        {
            streamOut.writeUTF(msg);
            streamOut.flush();
        }
        catch(IOException e)
        {
            System.out.println(ID + " ERROR sending: " + e.getMessage());
            server.Remove(ID);
        }
    }
    public int getID()
    {
        return ID;
    }
    public void run()
    {
        System.out.println("Server Thread " + ID + " running.");
        while (true)
        {
            try
            {
                if (accepted)
                {
                    String msg = streamIn.readUTF();
                    server.SendMessage(ID, msg);
                    if (msg.equals(".bye"))
                    {
                        break;
                    }
                }
                else
                {
                    Send("Name: ");
                    String tempName = streamIn.readUTF();
                    Send("Passwort: ");
                    String tempPasswort = streamIn.readUTF();
                    if (CheckAccount(tempName, tempPasswort))
                    {
                        name = tempName;
                        server.SendJoined(ID);
                        accepted = true;

                    }
                    else
                    {
                        Send("Access denied");
                        System.out.println("Stopping Server Thread " + ID);
                        break;
                    }
                }
            }
            catch(IOException e)
            {
                System.out.println(ID + " ERROR reading: " + e.getMessage());
                break;
            }
        }
        server.Remove(ID);
    }
    public void open() throws IOException
    {
        streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }
    public void close() throws IOException
    {
        if (socket != null)
        {
            socket.close();
        }
        if (streamIn != null)
        {
            streamIn.close();
        }
        if (streamOut != null)
        {
            streamOut.close();
        }
    }
    public boolean CheckAccount(String name, String passwort)
    {
        boolean check = false;
        File file = new  File("./Accounts");
        file.mkdir();
        Boolean exist = new File("./Accounts/"+ name +".txt").isFile();
        try
        {
            if (!exist)
            {
                File fileAccount = new File("./Accounts/" + name + ".txt");
                fileAccount.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileAccount));
                writer.write(passwort);
                writer.close();
                check = true;
                System.out.println("New Account " + name + " made");
            }
            else
            {

                BufferedReader in = new BufferedReader(new FileReader("./Accounts/" + name + ".txt"));
                String inhalt = in.readLine();
                if (inhalt.equals(passwort))
                {
                    check = true;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return check;
    }
}