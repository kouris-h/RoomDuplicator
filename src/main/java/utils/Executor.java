package utils;

import gearth.extensions.ExtensionBase;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Executor {
    private final PacketInfoManager packetInfoManager;
    private final ExtensionBase extension;
    private final List<AwaitingPacket> awaitingPackets = new ArrayList<>();
    private final Object lock = new Object();

    public Executor(ExtensionBase extension) {
        packetInfoManager = extension.getPacketInfoManager();
        this.extension = extension;

        extension.intercept(HMessage.Direction.TOSERVER, this::onMessageToServer);
        extension.intercept(HMessage.Direction.TOCLIENT, this::onMessageToClient);
    }

    public void sendToServer(String hashOrName, Object... objects) {
        extension.sendToServer(new HPacket(hashOrName, HMessage.Direction.TOSERVER, objects));
    }

    public void sendToClient(String hashOrName, Object... objects) {
        extension.sendToClient(new HPacket(hashOrName, HMessage.Direction.TOCLIENT, objects));
    }

    private void onMessageToServer(HMessage hMessage) {
        PacketInfo info = packetInfoManager.getPacketInfoFromHeaderId(HMessage.Direction.TOSERVER, hMessage.getPacket().headerId());
        if(info == null) {
            return;
        }
        synchronized(lock) {
            awaitingPackets.stream()
                    .filter(packet -> packet.direction.equals(HMessage.Direction.TOSERVER)) // Filter TOSERVER packets
                    .filter(packet -> packet.headerName.equals(info.getName()))             // Filter to packets with matching headernames
                    .forEach(packet -> {
                        if(packet.test(hMessage)) {
                            packet.setPacket(hMessage.getPacket());
                        }
                    });
        }
    }

    private void onMessageToClient(HMessage hMessage) {
        PacketInfo info = packetInfoManager.getPacketInfoFromHeaderId(HMessage.Direction.TOCLIENT, hMessage.getPacket().headerId());
        if(info == null) {
            return;
        }
        synchronized(lock) {
            awaitingPackets.stream()
                    .filter(packet -> packet.direction.equals(HMessage.Direction.TOCLIENT)) // Filter TOCLIENT packets
                    .filter(packet -> packet.headerName.equals(info.getName()))             // Filter to packets with matching headernames
                    .forEach(packet -> {
                        if (packet.test(hMessage)) {
                            packet.setPacket(hMessage.getPacket());
                        }
                    });
        }
    }

    public HPacket awaitPacket(AwaitingPacket... packets) {
        synchronized(lock) {
            this.awaitingPackets.addAll(Arrays.asList(packets));
        }

        while (true) {
            for (AwaitingPacket awaitingPacket : packets) {
                if (awaitingPacket.isReady()) {
                    synchronized (lock) {
                        awaitingPackets.removeAll(Arrays.asList(packets));
                    }
                    return awaitingPacket.getPacket();
                }
            }
        }
    }

    public void clear() {
        synchronized(lock) {
            this.awaitingPackets.clear();
        }
    }

    public List<HPacket> awaitPacketList(AwaitingPacket... packets) {
        synchronized (lock) {
            this.awaitingPackets.addAll(Arrays.asList(packets));
        }

        while(true) {
            if(Arrays.stream(packets).allMatch(AwaitingPacket::isReady)) {
                synchronized (lock) {
                    awaitingPackets.removeAll(Arrays.asList(packets));
                }
                return Arrays.stream(packets).map(AwaitingPacket::getPacket).collect(Collectors.toList());
            }
        }
    }

    public static class AwaitingPacket {
        public final String headerName;
        public final HMessage.Direction direction;
        private HPacket packet = null;
        private boolean received = false;
        private final List<Predicate<? super HPacket>> conditions = new ArrayList<>();
        private final long start;
        private long minWait = 0;

        public AwaitingPacket(String headerName, HMessage.Direction direction, int maxWaitingTimeMillis) {
            this.headerName = headerName;
            this.direction = direction;

            if(maxWaitingTimeMillis < 50) {
                maxWaitingTimeMillis = 50;
            }

            AwaitingPacket packet = this;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    packet.received = true;
                }
            }, maxWaitingTimeMillis);

            this.start = System.currentTimeMillis();
        }

        public AwaitingPacket setMinWaitingTime(int millis) {
            this.minWait = millis;
            return  this;
        }

        @SafeVarargs
        public final AwaitingPacket addConditions(Predicate<? super HPacket>... conditions) {
            this.conditions.addAll(Arrays.asList(conditions));
            return this;
        }

        private void setPacket(HPacket packet) {
            this.packet = packet;
            received = true;
        }

        public HPacket getPacket() {
            if(packet != null) {
                this.packet.resetReadIndex();
            }
            return this.packet;
        }

        private boolean test(HMessage hMessage) {
            for(Predicate<? super HPacket> condition : conditions) {
                HPacket packet = hMessage.getPacket();
                packet.resetReadIndex();
                if(condition.test(packet)) {
                    return true;
                }
            }
            return conditions.isEmpty();
        }

        private boolean isReady() {
            return received && start + minWait < System.currentTimeMillis();
        }
    }
}

