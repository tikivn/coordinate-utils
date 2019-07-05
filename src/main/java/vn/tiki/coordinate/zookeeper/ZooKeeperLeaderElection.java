package vn.tiki.coordinate.zookeeper;

import java.util.Collections;
import java.util.List;

import lombok.Getter;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import vn.tiki.coordinate.AbstractLeaderElection;
import vn.tiki.coordinate.Member;
import vn.tiki.coordinate.exception.LeaderElectionException;

@Slf4j
public class ZooKeeperLeaderElection extends AbstractLeaderElection {

    private static final String PREFIX = "candidate_";

    private final ZooKeeper zooKeeper;
    private final String rootNodePath;

    @Getter
    private String path;

    @Getter
    private String leaderPath;

    public ZooKeeperLeaderElection(ZooKeeper zooKeeper, String rootNodePath) {
        this.zooKeeper = zooKeeper;
        this.rootNodePath = rootNodePath;
        if (this.zooKeeper != null) {
            this.init();
        }
    }

    void init() {
        String rootPath = this.createNode(this.rootNodePath, false, false, null); // create root node
        if (rootPath == null) {
            throw new LeaderElectionException("Cannot get/create root path");
        }
        this.refresh();
    }

    // Verify node existing, then return, or not create new node and return the NodePath
    String createNode(final String node, final boolean watch, final boolean ephemeral, byte[] data) {
        String createdNodePath = null;
        int tryCountdown = 10;
        while (tryCountdown-- > 0) {
            try {
                final Stat nodeStat = zooKeeper.exists(node, watch);
                if (nodeStat == null) {
                    createdNodePath = zooKeeper.create(node, data, Ids.OPEN_ACL_UNSAFE,
                            (ephemeral ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.PERSISTENT));
                } else {
                    createdNodePath = node;
                }
                break;
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted", e);
            } catch (KeeperException e) {
                if (tryCountdown == 0) {
                    throw new RuntimeException("Cannot create node: " + node + ", ephemeral: " + ephemeral, e);
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return createdNodePath;
    }

    // Function for received refresh event
    private void refresh() {
        try {
            // Register the watcher for the next (or first) time.
            List<String> childNodePaths = zooKeeper.getChildren(this.rootNodePath, event -> {
                refresh();
            });
            Collections.sort(childNodePaths);

            if (alreadyNominatedSuccess()) {
                handleNominatedCase(childNodePaths);
            }

            String newLeaderPath = childNodePaths.size() == 0 ? null : this.rootNodePath + "/" + childNodePaths.get(0);
            if (needUpdateLeader(newLeaderPath)) {
                this.leaderPath = newLeaderPath;
                registerWatcherForLeaderDataChange();
                buildAndUpdateLeaderThenTriggerEvent();
            }
        } catch (InterruptedException e) {
            throw new LeaderElectionException(e);
        } catch (KeeperException e) {
            if (e.code() == Code.CONNECTIONLOSS) {
                // Do nothing in this case!
            } else if (e.code() != Code.SESSIONEXPIRED) {
                throw new LeaderElectionException(e);
            }
        }
    }

    private boolean alreadyNominatedSuccess() {
        return  this.path != null;
    }

    private void handleNominatedCase(List<String> childNodePaths) throws KeeperException, InterruptedException {
        String nodeName = path.substring(path.lastIndexOf('/') + 1);
        int index = childNodePaths.indexOf(nodeName);
        if (index == 0) {
            this.setLeaderAndTriggerEvent(this.getCandidate());
        }
    }

    private boolean needUpdateLeader(String newLeaderPath) {
        return this.leaderPath == null || !this.leaderPath.equals(newLeaderPath);
    }

    private void registerWatcherForLeaderDataChange() throws KeeperException, InterruptedException {
        if (leaderPath != null) {
            if (this.getCandidate() != null && !this.getCandidate().equals(this.getLeader())) {
                // if I'm not the leader, watch him for data change
                zooKeeper.exists(leaderPath, event -> {
                    if (EventType.NodeDataChanged.equals(event.getType())) {
                        buildAndUpdateLeaderThenTriggerEvent();
                    }
                });
            }
        }
    }

    private void buildAndUpdateLeaderThenTriggerEvent() {
        Member newLeader;
        try {
            newLeader = buildMemberFromZookeeper(leaderPath);
        } catch (KeeperException | InterruptedException e) {
            log.error("Error while trying to get new leader info...", e);
            return;
        }

        this.setLeaderAndTriggerEvent(newLeader);
    }

    private Member buildMemberFromZookeeper(String nodePath) throws KeeperException, InterruptedException {
        Member member = null;
        if (nodePath != null) {
            byte[] bytes = this.zooKeeper.getData(nodePath, true, null);
            member = Member.fromBytes(bytes);
        }
        return member;
    }

    @Override
    protected boolean tryNominateCandidate(@NonNull Member member) throws Exception {
        if (alreadyNominatedSuccess()) {
            throw new LeaderElectionException("Local candidate already exist, cannot nominate other one");
        }
        this.path = createNode(rootNodePath + "/" + PREFIX, false, true, member.toBytes());
        return path != null;
    }

    @Override
    public synchronized void cancelCandidate() {
        if (alreadyNominatedSuccess()) {
            try {
                this.zooKeeper.delete(this.path, 0);
                this.path = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


}
