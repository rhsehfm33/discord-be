package discord.chat.api.infrastructure.redis;

import discord.chat.api.application.session.ChatSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionEventPublisher {
    private final ChatSessionRedisBroker chatSessionRedisBroker;

    public void publishAfterCommit(ChatSessionEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
            || !TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(event);
            }
        });
    }

    private void publish(ChatSessionEvent event) {
        try {
            chatSessionRedisBroker.publish(event);
        } catch (Exception exception) {
            log.error(
                "Chat session event publication failed: type={}, userId={}, chatRoomId={}",
                event.type(),
                event.userId(),
                event.chatRoomId(),
                exception
            );
        }
    }
}
