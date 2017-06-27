/*
 * Does all the checks that the messaging API performs prior to getting and sending any message to/from all the masters
 * Dumps information related to the cluster messaging database.
 * Only to be executer on CJOC.
 */

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;

import com.cloudbees.opscenter.context.Messaging;
import com.cloudbees.opscenter.context.Messaging.OutboxEntry;
import com.cloudbees.opscenter.server.messaging.Transport;
import com.cloudbees.opscenter.server.model.ConnectedMaster;

import hudson.ExtensionList;
import hudson.model.AsyncPeriodicWork;
import hudson.model.PeriodicWork;
import hudson.remoting.Channel;
import jenkins.model.Jenkins;


Jenkins jenkins = Jenkins.getInstance();
out.println("InstanceId: " + jenkins.getLegacyInstanceId());
Transport transport = getTransport();

try {
    for (ConnectedMaster m : jenkins.getAllItems(ConnectedMaster.class)) {
        String instanceId = m.getLegacyInstanceId();
        out.println("  " + m.getFullName() + "(" + instanceId + "):");
        out.println("     offline: " + m.isOffline());
        Channel channel = m.getChannel();
        out.println("     channel: " + channel);
        if (channel != null) {
            out.println("     channelClossing: " + channel.isClosingOrClosed());
        } else {
            continue;
        }

        out.println("     capabilities: " + m.checkCapability(Messaging.class));

        Method method;

        try {
            out.println("     batchSource: " + transport.getBatchSource(channel));
        } catch (Exception e) {
            out.println("        Error batchSource: " + e.toString());
            out.println("     batchSource: " + Messaging.batchSource());
        }

        try {
            out.println("     batchSink: " + transport.getBatchSink(channel));
        } catch (Exception e) {
            out.println("        Error batchSink: " + e.toString());
            out.println("     batchSink: " + Messaging.batchSink());
        }

        try {
            out.println("     reliableMessageTransport: " + transport.getReliableMessageTransport(channel));
        } catch (Exception e) {
            out.println("        Error reliableMessageTransport: " + e.toString());
            out.println("     reliableMessageTransport: " + Messaging.reliableMessageTransport());
        }
    }
} catch (Exception e) {
    e.printStackTrace(out);
}
final ClassLoader oldContext = Thread.currentThread().getContextClassLoader();

try {
    Thread.currentThread().setContextClassLoader(Jenkins.getInstance().getPluginManager().uberClassLoader);

    Map<ConnectedMaster, Map<Class<?>, Map<String, List<Object>>>> offlineBuffer;

    ConcurrentMap<String, Long> maxPull;
    ConcurrentMap<String, Long> minPush;
    ConcurrentNavigableMap<String, Messaging.OutboxEntry<?>> outbox;

    maxPull = transport.maxPull;
    minPush = transport.minPush;
    outbox = transport.outbox
    offlineBuffer = transport.offlineBuffer

    try {
        out.println("maxPulls:");
        Set<Entry<String, Long>> entrySet = maxPull.entrySet();
        for (Entry<String, Long> entry : entrySet) {
            out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }
    } catch (Exception e) {
        out.println("Could not get maxPulls information");
        e.printStackTrace(out);
    }

    try {
        out.println("minPush:");
        Set<Entry<String, Long>> entrySet = minPush.entrySet();
        for (Entry<String, Long> entry : entrySet) {
            out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }
    } catch (Exception e) {
        out.println("Could not get minPush information");
        e.printStackTrace(out);
    }

    try {
        out.println("outbox:");
        Set<Entry<String, OutboxEntry<?>>> entrySet = outbox.entrySet();

        for (Entry<String, OutboxEntry<?>> entry : entrySet) {
            OutboxEntry<?> value = entry.getValue();
            out.println("  - " + entry.getKey() + "- source:" + value.getAddress() +
                    " - destination:" + value.getDestinationId() +
                    " - expiry:" + new Date(value.getExpiry()) +
                    " - class:" + value.getClazzName());
        }
    } catch (Exception e) {
        out.println("Could not get minPulls information");
        e.printStackTrace(out);
    }

    try {
        Set<Entry<ConnectedMaster, Map<Class<?>, Map<String, List<Object>>>>> entrySet = offlineBuffer.entrySet();
        out.println("offlineBuffer: " + offlineBuffer.size());

        for (Entry<ConnectedMaster, Map<Class<?>, Map<String, List<Object>>>> entry : entrySet) {
            Map<Class<?>, Map<String, List<Object>>> messagesMap = entry.getValue();
            ConnectedMaster key = entry.getKey();
            out.println("  " + key.getFullDisplayName() + "(" + key.getLegacyInstanceId() + ") - " + messagesMap.size());

            for (Entry<Class<?>, Map<String, List<Object>>> messages : messagesMap.entrySet()) {
                Map<String, List<Object>> value = messages.getValue();
                out.println("    " + messages.getKey() + ":" + value.size());
                Set<Entry<String, List<Object>>> messages2 = value.entrySet();
                for (Entry<String, List<Object>> message : messages2) {
                    out.println("      " + message.getKey() + ":" + messages.getValue());
                }
            }
        }
    } catch (Exception e) {
        out.println("Could not get minPulls information");
        e.printStackTrace(out);
    }
} finally {
    Thread.currentThread().setContextClassLoader(oldContext);
}

Transport getTransport() {
    ExtensionList<PeriodicWork> all = AsyncPeriodicWork.all();
    for (PeriodicWork periodicWork : all) {
        if (periodicWork instanceof Transport) {
            return (Transport) periodicWork;
        }
    }
    return null;
}
