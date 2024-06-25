package org.example.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181"; // ZooKeeper 서버 주소
    private static final int SESSION_TIMEOUT = 3000; // 세션 타임아웃 설정 (밀리초)
    private static final String ELECTION_NAMESPACE = "/election"; // 선거 네임스페이스 경로
    private ZooKeeper zooKeeper;
    private String currentZNodeName;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        // LeaderElection 객체 생성
        LeaderElection leaderElection = new LeaderElection();
        // ZooKeeper에 연결
        leaderElection.connectToZookeeper();
        // 리더 선출을 위해 자원 봉사
        leaderElection.volunteerForLeadership();
        // 리더 선출
        leaderElection.electLeader();
        // 애플리케이션 실행
        leaderElection.run();
        // ZooKeeper 연결 종료
        leaderElection.close();
        System.out.println("Disconnected from ZooKeeper, exiting application"); // 종료 메시지 출력
    }

    public void connectToZookeeper() throws IOException, InterruptedException {
        CountDownLatch connectedSignal = new CountDownLatch(1); // 연결 신호 CountDownLatch 생성
        // ZooKeeper 객체 생성 및 연결
        zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) { // 연결 상태 확인
                connectedSignal.countDown(); // 연결 신호 감소, CountDownLatch 가 0 이 되면 대기중인 스레드가 깨어남
            }
        });
        connectedSignal.await(); // 연결 신호 대기
    }

    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_"; // znode 접두사 설정
        // 순차적이고 임시적인 znode 생성
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("znode name " + znodeFullPath); // 생성된 znode 경로 출력
        this.currentZNodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", ""); // 현재 znode 이름 설정
    }

    public void electLeader() throws KeeperException, InterruptedException {
        Stat predecessorStat = null;
        String predecessorZNodeName = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false); // 자식 노드 리스트 가져오기
            Collections.sort(children); // 자식 노드 리스트 정렬
            String smallestChild = children.get(0); // 가장 작은 자식 노드 선택

            if (smallestChild.equals(currentZNodeName)) { // 현재 노드가 가장 작은 노드인 경우
                System.out.println("I am the leader"); // 리더임을 출력
                return;
            } else {
                System.out.println("I am not the leader, " + smallestChild + " is the leader"); // 리더가 아님을 출력
                int predecessorIndex = Collections.binarySearch(children, currentZNodeName) - 1; // 이전 노드 인덱스 찾기
                predecessorZNodeName = children.get(predecessorIndex); // 이전 노드 이름 설정
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZNodeName, this); // 이전 노드 상태 확인 및 감시 설정
            }
        }
        System.out.println("Watching znode " + predecessorZNodeName); // 감시 중인 노드 출력
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait(); // ZooKeeper 객체가 종료될 때까지 대기
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close(); // ZooKeeper 연결 종료
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case NodeDeleted:
                try {
                    electLeader(); // 노드 삭제 이벤트 발생 시 리더 재선출
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace(); // 예외 발생 시 스택 트레이스 출력
                }
        }
    }
}