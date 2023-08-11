package org.datastax.simulacra.conversation;

import org.datastax.simulacra.agents.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversationsRegistry {
    private static final List<Conversation> conversations = new ArrayList<>();

    private static final AtomicInteger numConversations = new AtomicInteger();
    private static final AtomicInteger allConversations = new AtomicInteger();

    public static void addConversation(Agent instigator, Agent target) {
        numConversations.getAndIncrement();
        allConversations.getAndIncrement();

        Conversation.start(instigator, target).thenAccept(conversation -> {
            try {
                synchronized (conversations) {
                    conversations.add(conversation);
                    conversations.notifyAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void removeConversation(Conversation conversation) {
        synchronized (conversations) {
            conversations.remove(conversation);
        }
    }

    public static List<Conversation> getSnapshot() {
        List<Conversation> snapshot;

        synchronized (conversations) {
            snapshot = new ArrayList<>(conversations);
        }

        return snapshot;
    }

    public static int numConversations() {
        return numConversations.get();
    }

    public static int allConversations() {
        return allConversations.get();
    }

    public static void waitOnConversations() throws InterruptedException {
        synchronized (conversations) {
            while (conversations.isEmpty()) {
                conversations.wait();
            }
        }
    }
}
