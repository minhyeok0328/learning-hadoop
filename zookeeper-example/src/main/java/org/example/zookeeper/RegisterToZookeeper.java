package org.example.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.lang.management.ManagementFactory;

public class RegisterToZookeeper {
    public static void main(String[] args) throws Exception {
        // ZooKeeper에 연결
        ZooKeeper zk = new ZooKeeper("localhost:2181", 5000, null);

        // 프로세스 ID를 얻는다.
        String processId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        // /members 노드가 없으면 생성
        String membersPath = "/members";
        try {
            if (zk.exists(membersPath, false) == null) {
                zk.create(membersPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

        // 프로세스를 등록하기 위한 znode 생성
        String znodePath = membersPath + "/resource_m1" + processId;
        zk.create(znodePath,
                  processId.getBytes(),    // znode에 저장할 데이터. 여기서는 프로세스 ID를 사용
                  Ids.OPEN_ACL_UNSAFE,     // 모든 클라이언트에서 액세스 가능한 ACL
                  CreateMode.EPHEMERAL);   // 프로세스가 종료되면 znode가 자동 삭제되는 모드

        System.out.println("Registered to ZooKeeper as member " + processId);

        // 이 예에서는 프로세스가 종료되기 전에 일부 시간을 기다립니다.
        // 실제 애플리케이션에서는 필요에 따라 다른 로직을 수행합니다.
        Thread.sleep(Long.MAX_VALUE);
    }
}