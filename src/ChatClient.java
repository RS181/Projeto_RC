import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    private Socket clientSocket;

    private DataOutputStream msgToServer;

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

        
        try {
            clientSocket = new Socket(server, port);
            msgToServer = new DataOutputStream(clientSocket.getOutputStream());
        }catch(ConnectException e){
            System.out.println("Unable to connect to server");
            System.exit(0);
        }

    }

    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
        message.trim();
        System.out.println("SENDING: " + message);
        if (message.startsWith("/")){
            String[] tks = message.split("\\s");

            //Ponto 2 das Notas do enunciado
            if (tks[0].equals("/join") || tks[0].equals("/leave") || tks[0].equals("/nick") || tks[0].equals("/bye") || tks[0].equals("/priv")){
                msgToServer.write((message + "\n").getBytes("UTF-8"));
                return;
            }
            else {
                if (message.startsWith("//")){
                    message = "/" + message;
                }
                else {
                    message = "//notacommand";
                }
            }
        }
        
        msgToServer.write((message + "\n").getBytes("UTF-8"));

    }

    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        try {
            BufferedReader data;
            boolean flag = true;
            while (flag) {
                data = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String msg = data.readLine();
                System.out.println("RECEIVED: " + msg);

                if (msg == null) {
                    // System.out.println("Closing user window");
                    System.exit(0);
                    // flag = false;
                    // break;
                }

                String[] tks = msg.split(" ");

                // Mensagens enviadas pelo servidor
                switch (tks[0]) {
                    case "MESSAGE": {
                        msg = msg.replaceFirst("MESSAGE", "");
                        msg = msg.replaceFirst(tks[1], "");
                        msg = tks[1] + ":" + msg;
                        break;
                    }
                    case "NEWNICK": {
                        msg = tks[1] + " mudou de nome para " + tks[2];
                        break;
                    }
                    case "JOINED": {
                        msg = tks[1] + " entrou na sala";
                        break;
                    }
                    case "LEFT": {
                        msg = tks[1] + " saiu da sala";
                        break;
                    }
                    case "PRIVATE": {
                        break;
                    }
                }

                System.out.println("PRINTING: " + msg);

                if (msg.compareTo("BYE\n") == 0) {
                    flag = false;
                }
                msg = msg + "\n";

                printMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        

    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
