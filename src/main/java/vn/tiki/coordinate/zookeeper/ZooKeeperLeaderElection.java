package vn.tiki.coordinate.zookeeper;

import java.util.Collections;
import java.util.List;

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

    private String path;

    private String watchedNodePath;

    private String leaderPath;

    public ZooKeeperLeaderElection(ZooKeeper zooKeeper, String rootNodePath) {
        this.zooKeeper = zooKeeper;
        this.rootNodePath = rootNodePath;

        this.init();
    }

    public void init() {
        String rootPath = this.createNode(this.rootNodePath, false, false, null); // create root node
        if (rootPath == null) {
            throw new LeaderElectionException("Cannot get/create root path");
        }
        this.refresh();
    }

    private String createNode(final String node, final boolean watch, final boolean ephemeral, byte[] data) {
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
                throw new RuntimeException("Interupted", e);
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

    private void refresh() {
        try {
            List<String> childNodePaths = zooKeeper.getChildren(this.rootNodePath, event -> {
                refresh();
            });

            Collections.sort(childNodePaths);

            if (this.path != null) {
                String nodeName = path.substring(path.lastIndexOf('/') + 1);
                int index = childNodePaths.indexOf(nodeName);
                if (index == 0) {
                    this.setLeader(this.getCandidate());
                } else {
                    // If I'm not leader, try to watch on previous node
                    final String tobeWatchedNodePath = this.rootNodePath + "/" + childNodePaths.get(index - 1);
                    if (this.watchedNodePath == null || !tobeWatchedNodePath.equals(this.watchedNodePath)) {
                        watchedNodePath = tobeWatchedNodePath;
                        zooKeeper.exists(watchedNodePath, true);
                    }
                }
            }

            String newLeaderPath = childNodePaths.size() == 0 ? null : this.rootNodePath + "/" + childNodePaths.get(0);
            if (this.leaderPath == null || !this.leaderPath.equals(newLeaderPath)) {
                this.leaderPath = newLeaderPath;
                if (newLeaderPath != null) {
                    if (this.getCandidate() != null && this.getLeader() != this.getCandidate()) {
                        // if I'm not the leader, watch him for data change
                        zooKeeper.exists(leaderPath, event -> {
                            if (EventType.NodeDataChanged.equals(event.getType())) {
                                updateLeader();
                            }
                        });
                    }
                }
                updateLeader();
            }
        } catch (InterruptedException e) {
            throw new LeaderElectionException(e);
        } catch (KeeperException e) {
            if (e.code() != Code.SESSIONEXPIRED) {
                throw new LeaderElectionException(e);
            }
        }
    }

    private void updateLeader() {
        Member newLeader;
        if (leaderPath != null) {
            byte[] bytes;
            try {
                bytes = this.zooKeeper.getData(leaderPath, true, null);
                newLeader = Member.fromBytes(bytes);
            } catch (KeeperException | InterruptedException e) {
                log.error("Error while trying to get new leader info...", e);
                return;
            }
        } else {
            newLeader = null;
        }
        this.setLeader(newLeader);
    }

    @Override
    protected boolean tryNominateCandidate(@NonNull Member member) throws Exception {
        if (this.path != null) {
            throw new LeaderElectionException("Local candidate already exist, cannot nominate other one");
        }
        this.path = createNode(rootNodePath + "/" + PREFIX, false, true, member.toBytes());
        return path != null;
    }

    @Override
    public synchronized void cancelCandidate() {
        if (this.path != null) {
            try {
                this.zooKeeper.delete(this.path, 0);
                this.path = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
