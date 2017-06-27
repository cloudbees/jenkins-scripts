/*
 * Dumps information related to the local messaging database.
 * It can be used on CJE and CJOC.
 */

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.mapdb.Atomic;

import com.cloudbees.opscenter.context.Messaging;
import com.cloudbees.opscenter.context.Messaging.InboxEntry;
import com.cloudbees.opscenter.context.Messaging.InboxKey;
import com.cloudbees.opscenter.context.Messaging.OutboxEntry;

import jenkins.model.Jenkins;


out.println("InstanceId: " + Jenkins.getInstance().getLegacyInstanceId());
final ClassLoader oldContext = Thread.currentThread().getContextClassLoader();
try {
    Thread.currentThread().setContextClassLoader(Jenkins.getInstance().getPluginManager().uberClassLoader);
    Atomic.Long inboxSequenceId;
    Atomic.Long outboxSequenceId;
    SortedMap<InboxKey<?>, InboxEntry<?>> inbox;
    SortedMap<Long, OutboxEntry<?>> outbox;
    Messaging messaging = Messaging.getInstance();
    Field localField;

    localField = Messaging.class.getDeclaredField("local");
    localField.setAccessible(true);

    Object local = localField.get(messaging);
    Class localClass = Messaging.class.getDeclaredClasses()[1];// 1 is the index for Messaging.Local

    Field field = localClass.getDeclaredField("inbox");
    field.setAccessible(true);

    inbox = (SortedMap<InboxKey<?>, InboxEntry<?>>) field.get(local);

    field = localClass.getDeclaredField("outbox");
    field.setAccessible(true);
    outbox = (SortedMap<Long, OutboxEntry<?>>) field.get(local);

    field = localClass.getDeclaredField("inboxSequenceId");
    field.setAccessible(true);
    inboxSequenceId = (org.mapdb.Atomic.Long) field.get(local);

    field = localClass.getDeclaredField("outboxSequenceId");
    field.setAccessible(true);
    outboxSequenceId = (org.mapdb.Atomic.Long) field.get(local);

    try {
        out.println("outboxSequenceId:" + outboxSequenceId);
        out.println("outbox size: " + outbox.size());
        Set<Entry<Long, OutboxEntry<?>>> entrySet2 = outbox.entrySet();
        for (Entry<Long, OutboxEntry<?>> entry : entrySet2) {
            OutboxEntry<?> value = entry.getValue();
            out.println(entry.getKey() + " - "
                    + "Expiry:" + new Date(value.getExpiry()) + " - "
                    + "Destination:" + value.getDestinationId() + " - "
                    + "Class Name:" + value.getClazzName());
        }
    } catch (Exception e) {
        out.println("Could not get inbox information");
        e.printStackTrace(out);
    }
    try {
        out.println("inboxSequenceId:" + inboxSequenceId);
        out.println("inbox size: " + inbox.size());
        Set<Entry<InboxKey<?>, InboxEntry<?>>> entrySet = inbox.entrySet();
        for (Entry<InboxKey<?>, InboxEntry<?>> entry : entrySet) {
            InboxEntry<?> value = entry.getValue();
            out.println(entry.getKey() + " - "
                    + "Expiry:" + new Date(value.getExpiry()) + " - "
                    + "Destination:" + value.getMessage().getClass());
        }
    } catch (Exception e) {
        out.println("Could not get outbox information");
        e.printStackTrace(out);
    }
} finally {
    Thread.currentThread().setContextClassLoader(oldContext);
}