package org.datastax.simulacra.conversation;

import org.datastax.simulacra.agents.Agent;

import java.util.ArrayList;
import java.util.List;

public class ConversationsRegistry {
    private static final List<Conversation> conversations = new ArrayList<>();

    public static void addConversation(Agent instigator, Agent target) {
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

    public static boolean isEmpty() {
        synchronized (conversations) {
            return conversations.isEmpty();
        }
    }

    public static void waitOnConversations() throws InterruptedException {
        synchronized (conversations) {
            while (conversations.isEmpty()) {
                conversations.wait();
            }
        }
    }
}
