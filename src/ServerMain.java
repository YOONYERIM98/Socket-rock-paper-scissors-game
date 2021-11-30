import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class ServerMain {

  // 묵
  final static String MOOK = "mook";
  // 찌
  final static String JJI = "jji";
  // 빠
  final static String BBA = "bba";

  private ServerSocket serverSocket;

  private final Socket[] socketList = new Socket[4];
  private final String[] userMookJjiBba = new String[4];
  private final boolean[] loseGameUser = new boolean[]{true, true, true, true};
  private String cpuMookJjiBba = MOOK;

  private int currentUserNum = 0;

  ServerMain() {
    // 유저 묵찌빠 정보를 전부 null로 초기화시킨다.
    for (int i = 0; i < userMookJjiBba.length; i++) {
      userMookJjiBba[i] = null;
    }

    try {
      serverSocket = new ServerSocket(9000);

      // 게임 스레드를 동작시킨다.
      new Thread(() -> {
        try {
          System.out.println("run game thread");
          gameThread();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();

      // 소켓 접속 요청을 대기한다.
      while (true)
        socketWait(serverSocket);

    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("server socket cannot init");
    } finally {
      try {
        for (int i = 0; i < socketList.length; i++) {
          if (socketList[i] != null) {
            socketList[i].close();
          }
        }
        serverSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // 컴퓨터가 묵찌빠중 뭘낼지 결정하는 메서드
  private void computerMookJjiBba() {
    int rand = new Random().nextInt() % 3;
    if (rand == 0) {
      cpuMookJjiBba = MOOK;
    } else if (rand == 1) {
      cpuMookJjiBba = JJI;
    } else {
      cpuMookJjiBba = BBA;
    }
  }

  private void gameCalculateResult() throws IOException {
    // 컴퓨터와 묵찌빠를 할 경우, 승패를 정해주는 부분.
    if (currentUserNum == 1) {
      int i = 0;
      for (i = 0; i < socketList.length; i++) {
        if (socketList[i] != null && socketList[i].isConnected()) {//현재 접속한 사람이 한명이 누군인지 찾아내는
          break;
        }
      }
      final int idx = i;

      String mookJjiBba = userMookJjiBba[idx]; //찾아낸 그사람 값
      if (mookJjiBba == null) {
        sendData(idx, "lose");
        return;
      }

      if (mookJjiBba.equals(cpuMookJjiBba)) {
        // 무승부
        sendData(idx, "same");
      } else if (mookJjiBba.equals(MOOK)) {
        if (cpuMookJjiBba.equals(JJI)) {
          // 이김
          sendData(idx, "win");
        } else if (cpuMookJjiBba.equals(BBA)) {
          // 짐
          sendData(idx, "lose");
        }
      } else if (mookJjiBba.equals(JJI)) {
        if (cpuMookJjiBba.equals(MOOK)) {
          // 짐
          sendData(idx, "lose");
        } else if (cpuMookJjiBba.equals(BBA)) {
          // 이김
          sendData(idx, "win");
        }
      } else if (mookJjiBba.equals(BBA)) {
        if (cpuMookJjiBba.equals(JJI)) {
          // 짐
          sendData(idx, "lose");
        } else if (cpuMookJjiBba.equals(MOOK)) {
          // 이김
          sendData(idx, "win");
        }
      }

    } else {
      // 현재 유저들이 낸 모든 묵찌빠 정보를 문자열로 붙인다. 만약, 내지 않았다면 패배처리한다.
      // 문자열로 묶어버린 이유는, 승패처리를 할 때, 문자열.contains 메서드를 활용하기 위함이다.
      // 문자열.concats 을 사용하는 이유는 여러명 가위바위보를 했을 때, 가장 간단하게 할 수 있는 로직이라 생각되서이다.
      String concatAll = "";
      int userNum = 0;
      for (int idx = 0; idx < socketList.length; idx++) {
        if (socketList[idx] != null && socketList[idx].isConnected() && !loseGameUser[idx]) {
          if (userMookJjiBba[idx] == null) {
            sendData(idx, "lose");
            loseGameUser[idx] = true;
          } else {
            if (concatAll.equals("")) {
              concatAll += userMookJjiBba[idx];
            } else {
              concatAll += "," + userMookJjiBba[idx];
            }
            userNum++;
          }
        }
      }
      if(userNum == 0){
        return;
      }

      if(userNum == 1){
        sendToWinnerOrLose(concatAll);
        return;
      }

      String winnerItem = "";
      String resultState = "";
      if (concatAll.contains(MOOK)) {
        if (concatAll.contains(JJI)) {
          if (concatAll.contains(BBA)) {
            // 무승부
            resultState = "same";
            sendAllForNotLoseUser(resultState);
            return;
          } else {
            // 묵 찌
            winnerItem = MOOK;
            sendToWinnerOrLose(winnerItem);
          }
        } else if (concatAll.contains(BBA)) {
          // 묵, 빠
          winnerItem = BBA;
          sendToWinnerOrLose(winnerItem);
        } else {
          // 묵밖에 없음
          sendAllForNotLoseUser("same");
        }
        return;
      }

      if (concatAll.contains(JJI)) {
        if (concatAll.contains(BBA)) {
          // 찌, 빠
          winnerItem = JJI;
          sendToWinnerOrLose(winnerItem);
        } else {
          // 찌밖에 없음
          sendAllForNotLoseUser("same");
        }
        return;
      }

      if (concatAll.contains(BBA)) {
        // 빠밖에 없음
        sendAllForNotLoseUser("same");
        return;
      }
    }
  }


  // 승리한 아이템에 따른 win , lose 처리를 하고 대상들에게 결과를 알려주는 메서드.
  private void sendToWinnerOrLose(String winnerItem) {
    for (int i = 0; i < loseGameUser.length; i++) {
      if (userMookJjiBba[i] != null && !loseGameUser[i]) {
        if (userMookJjiBba[i].equals(winnerItem)) {
          sendData(i, "win");
        } else {
          loseGameUser[i] = true;
          sendData(i, "lose");
        }
      }
    }
  }


  // 모든 유저들에게 메세지를 보내기 위한 메서드
  private void sendAll(String message) {
    for (int i = 0; i < socketList.length; i++) {
      if (socketList[i] != null && socketList[i].isConnected()) {
        sendData(i, message);
      }
    }
  }

  // 패배하지 않은 유저들에게만 데이터를 보내기 위한 메서드.
  private void sendAllForNotLoseUser(String message) {
    for (int i = 0; i < socketList.length; i++) {
      if (socketList[i] != null && socketList[i].isConnected() && !loseGameUser[i]) {
        sendData(i, message);
      }
    }
  }

  private void gameThread() throws InterruptedException, IOException {
    boolean isRun = false;
    while (true) {
      System.out.println("game rerun");
      // 현재 접속중인 유저가 존재하지 않다면, 1초마다 체크한뒤에 게임을 시작시킨다.
      for (int i = 0; i < socketList.length; i++) {
        if (socketList[i] != null && socketList[i].isConnected()) {
          isRun = true;
          break;
        } else {
          isRun = false;
        }
      }
      if (!isRun) {
        Thread.sleep(1000);
        System.out.println("접속한 유저가 없습니다.");
        continue;
      }
      sendAllForNotLoseUser("timerStart");
      Thread.sleep(6000);

      // 현재 존재하는 유저의 수가 1명이면, 컴퓨터와 가위바위보를 한다.
      if (currentUserNum == 1) {
        // 컴퓨터와 가위바위보
        computerMookJjiBba();
        for (int i = 0; i < userMookJjiBba.length; i++) {
          if (userMookJjiBba[i] != null) {
            sendForAllUserMookJjiBbaData();
          }
        }
        sendAll("cpu:" + cpuMookJjiBba);


      } else {
        // 유저들과 가위바위보
        sendForAllUserMookJjiBbaData();
      }
      // 가위바위보 결과를 도출해내고 결과에 맞게 전송한다.
      Thread.sleep(100);
      gameCalculateResult();
      Thread.sleep(100);

      // winner가 한명 혹은 모두 패배인지 체크하고, 한명 혹은 모두 패배라면 게임을 리셋한다.
      int winnerNum = 0;
      for (int i = 0; i < loseGameUser.length; i++) {
        if (!loseGameUser[i]) {
          winnerNum++;
        }
      }
      if (winnerNum <= 1) {
        gameReset();
      }

      // 모든 유저의 묵찌빠를 냈던 데이터를 null로 초기화시킨다.
      for (int i = 0; i < userMookJjiBba.length; i++) {
        userMookJjiBba[i] = null;
      }
      Thread.sleep(2000);

    }
  }

  // 게임의 최종 승리자가 정해졌으므로, 게임을 리셋하는 메서드이다.
  private void gameReset() {
    for (int i = 0; i < loseGameUser.length; i++) {
      if (socketList[i] != null && socketList[i].isConnected()) {
        loseGameUser[i] = false;
      }
    }
  }

  // 묵찌빠 데이터를 다른 유저들에게 전송하는 메서드이다.
  private void sendForAllUserMookJjiBbaData() {
    for (int i = 0; i < userMookJjiBba.length; i++) {
      if (userMookJjiBba[i] != null && !loseGameUser[i]) {
        sendAll(i + "p:" + userMookJjiBba[i]);
      }
    }
  }

  // 데이터를 보내는 메서드이다.
  private void sendData(final int idx, String message) {
    try {
      final Socket targetSocket = socketList[idx];
      // 만약, 해당 소켓이 null값이거나, disconnect 된 소켓이라면, 디스커넥트됨을 알리고 소켓을 종료한다.
      if (socketList[idx] == null || !socketList[idx].isConnected()) {
        sendAll(idx + "pDisconnected");
        socketList[idx].close();
        socketList[idx] = null;
        return;
      }

      // 데이터를 클라이언트에 전송한다.
      BufferedWriter bufWriter =
          new BufferedWriter(new OutputStreamWriter(targetSocket.getOutputStream()));
      System.out.println("send data to idx : " + idx + " // msg ? " + message);
      bufWriter.write(message);
      bufWriter.newLine();
      bufWriter.flush();
    } catch (IOException e) {
      // 전송도중 익셉션이 발생했으면, 소켓이 닫힌것이므로, disconnect 관련 작업을 해준다.
      if (socketList[idx] == null || !socketList[idx].isConnected()) {
        sendAll(idx + "pDisconnected");
        try {
          socketList[idx].close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
        socketList[idx] = null;
        return;
      }
    }
  }

  // 유저에게 받은 데이터를 처리하는 메서드.
  private void getGameData(final int idx) {
    Socket targetSocket = socketList[idx];
    try {
      while (true) {
        BufferedReader bufReader =
            new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));
        String message = bufReader.readLine();
        if (message == null) {
          break;
        }

        // 유저에게 mook, jji, bba중 하나를 받으면 묵찌빠 어레이에서 유저에 해당하는 인덱스에 해당 정보를 저장한다.
        if (message.equals(MOOK) || message.equals(JJI) || message.equals(BBA)) {
          userMookJjiBba[idx] = message;
        }

        System.out.println("[" + idx + "]" + message);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      // 만약, 위 과정이 종료되면, 소켓이 닫힌 것으로 간주하고, 초기화할 것들을 초기화한다.
      System.out.println("close socket!!");
      try {
        socketList[idx].close();
      } catch (IOException ex) {
      }
      socketList[idx] = null;
      loseGameUser[idx] = true;
      currentUserNum--;
      sendAll(idx + "pDisconnected");
    }
  }

  // 소켓 접속시 요청을 대기하는 메서드이다.
  private void socketWait(ServerSocket serverSocket) {
    try {
      // 소켓이 연결되면, socket list에서 비어있는 부분에 해당 소켓을 저장시킨다.
      Socket socket = serverSocket.accept();
      currentUserNum++;
      int idx = 0;
      while (socketList[idx] != null) {
        idx++;
      }
      socketList[idx] = socket;
      // 유저의 lose 정보를 false로 바꾼다.
      loseGameUser[idx] = false;
      final int currentIdx = idx;

      // 메세지 유실을 막기 위해 잠시간 대기해 준 후에, 유저의 플레이어 넘버를 보낸다.
      Thread.sleep(500);
      sendData(idx, String.valueOf(idx));

      // 모든 유저에게 커넥션된 유저에 대한 메세지를 날린다.
      Thread.sleep(100);
      for (int i = 0; i < 4; i++) {
        if (socketList[i] != null && socketList[i].isConnected()) {
          sendAll(i + "pConnected");
        }
      }

      // 유저에게 데이터를 받을 스레드를 생성하고 시작한다.
      new Thread(() -> {
        getGameData(currentIdx);
      }).start();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new ServerMain();
  }
}