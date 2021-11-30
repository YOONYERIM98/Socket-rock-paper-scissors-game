import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class ClientMain extends JFrame implements ActionListener {
  private Socket socket;
  static final ImageIcon mookIcon = new ImageIcon(new ImageIcon("./images/mook.png").getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH));
  static final ImageIcon jjiIcon = new ImageIcon(new ImageIcon("./images/jji.png").getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH));
  static final ImageIcon bbaIcon = new ImageIcon(new ImageIcon("./images/bba.png").getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH));

  private JLabel label1P = new JLabel("", SwingConstants.LEFT);
  private JLabel label1PMookJjiBba = new JLabel("", SwingConstants.LEFT);
  private JLabel label2P = new JLabel("cpu", SwingConstants.RIGHT);
  private JLabel label2PMookJjiBba = new JLabel("", SwingConstants.RIGHT);
  private JLabel label3P = new JLabel("", SwingConstants.LEFT);
  private JLabel label3PMookJjiBba = new JLabel("", SwingConstants.LEFT);
  private JLabel label4P = new JLabel("", SwingConstants.RIGHT);
  private JLabel label4PMookJjiBba = new JLabel("", SwingConstants.RIGHT);

  private JLabel leftTime = new JLabel("5", SwingConstants.CENTER);

  private JLabel winNumLabel = new JLabel("0", SwingConstants.RIGHT);
  private JLabel sameNumLabel = new JLabel("0", SwingConstants.RIGHT);
  private JLabel loseNumLabel = new JLabel("0", SwingConstants.RIGHT);

  private int myPlayerNumber = 0;

  private JButton mookButton = new JButton();
  private JButton jjiButton = new JButton();
  private JButton bbaButton = new JButton();


  // 묵
  final static String MOOK = "mook";
  // 찌
  final static String JJI = "jji";
  // 빠
  final static String BBA = "bba";

  private int roomUserNum = 0;

  ClientMain() {
    setTitle("가위,바위,보 게임");

    JPanel mainJPanel = new JPanel(new BorderLayout());


    mainJPanel.add(mookJjiBbaMainPanel(), BorderLayout.CENTER);//플레이어 나오는 패널
    mainJPanel.add(mookJjiBbaPanel(), BorderLayout.SOUTH);//가위바위보 이미지 선택
    mainJPanel.add(winLoseSamePanel(), BorderLayout.NORTH);//승무패나오는 패널

    // 윈도우가 종료될 때, 서버에 disconnect 메세지를 보내고 소켓연결을 종료한다.
    addWindowListener(new WindowAdapter() {
      public void windowClosed(WindowEvent e) {
      }

      public void windowClosing(WindowEvent e) {
        try {
          System.out.println("close");
          sendData("disconnect");
          socket.close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    });

    socketConnect();
    add(mainJPanel);
    pack();
    setVisible(true);
  }

  private JPanel winLoseSamePanel(){
    JPanel panel = new JPanel(new GridLayout(1,6));

    panel.add(winNumLabel);
    panel.add(new JLabel("승"));
    panel.add(sameNumLabel);
    panel.add(new JLabel("무"));
    panel.add(loseNumLabel);
    panel.add(new JLabel("패"));

    return panel;
  }

  // 플레이어 패널을 생성하는 메서드이다.
  // 플레이어 패널은 플레이어 넘버와 묵찌빠 어떤 것을 냈는지 보여주는 것들이 속성으로 있다.
  private JPanel playerPanel(int playerIdx) {
    JPanel playerPanel = new JPanel(new GridLayout(1, 2));
    if (playerIdx == 0) {
      playerPanel.add(label1P);
      playerPanel.add(label1PMookJjiBba);
    } else if (playerIdx == 1) {
      playerPanel.add(label2PMookJjiBba);
      playerPanel.add(label2P);
    } else if (playerIdx == 2) {
      playerPanel.add(label3P);
      playerPanel.add(label3PMookJjiBba);
    } else {
      playerPanel.add(label4PMookJjiBba);
      playerPanel.add(label4P);
    }
    playerPanel.setPreferredSize(new Dimension(200, 100));
    return playerPanel;
  }

  // 묵찌빠 경기 내용이 보이는 메인 패널을 생성하는 메서드이다.
  private JPanel mookJjiBbaMainPanel() {
    JPanel mainPanel = new JPanel(new GridLayout(3, 3));
//    JLabel emptyLabel = new JLabel("mep");

    mainPanel.add(playerPanel(0));

    mainPanel.add(leftTime);

    mainPanel.add(playerPanel(1));
    mainPanel.add(new JLabel());

    JLabel vsLabel = new JLabel("vs", SwingConstants.CENTER);
    mainPanel.add(vsLabel);
    mainPanel.add(new JLabel());

    mainPanel.add(playerPanel(2));

    mainPanel.add(new JLabel());

    mainPanel.add(playerPanel(3));

    return mainPanel;
  }

  // 묵찌파 패널을 생성하고 버튼들을 추가하는 메서드이다.
  private JPanel mookJjiBbaPanel() {
    JPanel mookJjiBbaPanel = new JPanel(new GridLayout(1, 3));

    mookButton.setIcon(mookIcon);
    mookButton.setContentAreaFilled(true);
    mookButton.addActionListener(this);
    mookJjiBbaPanel.add(mookButton);

    jjiButton.setIcon(jjiIcon);
    jjiButton.setContentAreaFilled(true);
    jjiButton.addActionListener(this);
    mookJjiBbaPanel.add(jjiButton);

    bbaButton.setIcon(bbaIcon);
    bbaButton.setContentAreaFilled(true);
    bbaButton.addActionListener(this);
    mookJjiBbaPanel.add(bbaButton);


    return mookJjiBbaPanel;
  }

  // 소켓을 연결한다.
  private void socketConnect() {
    try {
      socket = new Socket("localhost", 9000);
      getPlayerNumber();
      new Thread(() -> {
        getDataThread();
      }).start();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // 데이터를 서버에 전송한다.
  private void sendData(String message) {
    try {
      BufferedWriter bufWriter =
          new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      bufWriter.write(message);
      bufWriter.newLine();
      bufWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // 플레이어 넘버를 받아온다. 플레이어 넘버는 최초 연결 이후, 처음으로 날라오는 메세지이다.
  private void getPlayerNumber() {
    try {
      BufferedReader bufReader =
          bufReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String message = bufReader.readLine();
      myPlayerNumber = Integer.valueOf(message);

      getPlayerLabel(myPlayerNumber).setText((myPlayerNumber + 1) + "p(me)");


    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void getDataThread() {
    try {
      while (true) {
        BufferedReader bufReader =
            new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String message = bufReader.readLine();

        System.out.println("Message : " + message);
        // 타이머 스타트는 게임이 시작되었을 때, 남은 시간을 카운팅 한다.
        if (message.equals("timerStart")) {
          getPlayerMookJjiBbaLabel(myPlayerNumber).setIcon(null);
          new Thread(() -> {
            try {
              leftTime.setText("5");
              Thread.sleep(1000);
              leftTime.setText("4");
              // 카운팅하다 4초가 남았을 경우, 이전승부 정보에 대한 것을 모두 지운다.
              for (int i = 0; i < 4; i++) {
                if (i != myPlayerNumber) {
                  getPlayerMookJjiBbaLabel(i).setIcon(null);
                }
              }
              Thread.sleep(1000);
              leftTime.setText("3");
              Thread.sleep(1000);
              leftTime.setText("2");
              Thread.sleep(1000);
              leftTime.setText("1");
              Thread.sleep(1000);
              leftTime.setText("0");
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }).start();
        }

        // 다른 플레이어가 나가면 처리하는 부분이다.
        if (message.contains("pDisconnected")) {
          int playerNumber = message.charAt(0) - '0';
          if (myPlayerNumber != playerNumber) {
            getPlayerLabel(playerNumber).setText("");
            winNumLabel.setText("0");//이긴 수를 리셋
            loseNumLabel.setText("0");//패배 한수를 리셋
            sameNumLabel.setText("0");//무승부를 리셋
            roomUserNum--;
          }
          // 자신밖에 안남았을 경우, cpu 생성. 해당 cpu는 자신의 플레이어넘버를 제외한 가장 빠른 플레이어 넘버가 된다.
          if (roomUserNum == 0) {
            int i = 0;
            for (i = 0; i < 4; i++) {
              if (i != myPlayerNumber) {
                break;
              }
            }
            getPlayerLabel(i).setText("cpu");
          }
        }


        // 다른 플레이어가 입장하면, 처리하는 부분이다.
        if (message.contains("pConnected")) {
          int playerNumber = message.charAt(0) - '0';
          if (myPlayerNumber != playerNumber) {
            getPlayerLabel(playerNumber).setText((playerNumber + 1) + "p");
            winNumLabel.setText("0");//이긴 수를 리셋
            loseNumLabel.setText("0");//패배 한수를 리셋
            sameNumLabel.setText("0");//무승부를 리셋
            roomUserNum++;
          }
        }

        if (message.equals("win")) {
          // 승리
          winNumLabel.setText(String.valueOf(Integer.valueOf(winNumLabel.getText())+1));
        } else if (message.equals("lose")) {
          // 졌음
          loseNumLabel.setText(String.valueOf(Integer.valueOf(loseNumLabel.getText())+1));
        } else if (message.equals("same")) {
          // 무승부
          sameNumLabel.setText(String.valueOf(Integer.valueOf(sameNumLabel.getText())+1));
        }

        // 다른 유저가 묵찌빠중 어떤 것을 냈는지에 대한 정보가 날라오면 처리하는 부분.
        if (message.contains(myPlayerNumber + "p")) {
          continue;
        } else if (message.contains("cpu")) {
          // 컴퓨터일 경우, 자신에 해당하는 플레이어넘버를 제외한 다른 빠른 숫자중 하나에 해당 아이콘을 띄운다.
          int i = 0;
          for (i = 0; i < 4; i++) {
            if (i != myPlayerNumber) {
              break;
            }
          }
          if (message.contains(MOOK)) {
            getPlayerMookJjiBbaLabel(i).setIcon(mookIcon);
          } else if (message.contains(JJI)) {
            getPlayerMookJjiBbaLabel(i).setIcon(jjiIcon);
          } else if (message.contains(BBA)) {
            getPlayerMookJjiBbaLabel(i).setIcon(bbaIcon);
          }
        } else if (message.contains("0p") || message.contains("1p") || message.contains("2p") || message.contains("3p")) {
          int gettingPlayerNumber = message.charAt(0) - '0';
          if (message.contains(MOOK)) {
            getPlayerMookJjiBbaLabel(gettingPlayerNumber).setIcon(mookIcon);
          } else if (message.contains(JJI)) {
            getPlayerMookJjiBbaLabel(gettingPlayerNumber).setIcon(jjiIcon);
          } else if (message.contains(BBA)) {
            getPlayerMookJjiBbaLabel(gettingPlayerNumber).setIcon(bbaIcon);
          }
        }
      }
    } catch (IOException e) {
      try {
        socket.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      socket = null;
    }
  }

  // 인덱스에 해당하는 묵찌빠 라벨을 가져온다.
  private JLabel getPlayerMookJjiBbaLabel(int idx) {
    if (idx == 0) {
      return label1PMookJjiBba;
    } else if (idx == 1) {
      return label2PMookJjiBba;
    } else if (idx == 2) {
      return label3PMookJjiBba;
    } else {
      return label4PMookJjiBba;
    }
  }

  // 인덱스에 해당하는 플레이어 넘버 라벨을 가져온다.
  private JLabel getPlayerLabel(int idx) {
    if (idx == 0) {
      return label1P;
    } else if (idx == 1) {
      return label2P;
    } else if (idx == 2) {
      return label3P;
    } else {
      return label4P;
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // 묵,찌,빠 버튼을 눌렀을 때, 자신의 위치에 해당하는 묵찌빠라벨을 변경시키고 ,서버에 데이터를 전송한다.
    if (e.getSource() == mookButton) {
      getPlayerMookJjiBbaLabel(myPlayerNumber).setIcon(mookIcon);
      sendData(MOOK);
    }
    if (e.getSource() == jjiButton) {
      getPlayerMookJjiBbaLabel(myPlayerNumber).setIcon(jjiIcon);
      sendData(JJI);
    }
    if (e.getSource() == bbaButton) {
      getPlayerMookJjiBbaLabel(myPlayerNumber).setIcon(bbaIcon);
      sendData(BBA);
    }
  }

  public static void main(String[] args) {
    new ClientMain();
  }
}