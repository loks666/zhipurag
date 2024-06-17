let currentRequestId = null;

function newChat() {
    document.getElementById('messages').innerHTML = '';
    currentRequestId = null; // 清除当前的 requestId
}

function appendMessage(content, sender) {
    const messageContainer = document.createElement('div');
    messageContainer.className = `message ${sender}`;
    const messageBubble = document.createElement('div');
    messageBubble.className = 'message-bubble';
    messageBubble.innerHTML = content.replace(/\n/g, '<br>');
    messageContainer.appendChild(messageBubble);

    const messagesContainer = document.getElementById('messages');
    messagesContainer.appendChild(messageContainer);
    messagesContainer.scrollTop = messagesContainer.scrollHeight; // 自动滚动到最新消息

    if (sender === 'user') {
        messageContainer.style.alignSelf = 'flex-end';
    } else {
        messageContainer.style.alignSelf = 'flex-start';
    }
}

async function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value;
    if (message.trim() === '') return;

    appendMessage(message, 'user');
    input.value = '';

    // 显示等待图标
    const loadingElement = document.getElementById('loading');
    loadingElement.style.display = 'flex';

    let url = '/chat';
    if (currentRequestId) {
        url += `?requestId=${currentRequestId}`;
    }

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ user: message })
        });

        const result = await response.json();
        currentRequestId = result.requestId; // 更新 requestId
        appendMessage(result.system, 'ai');
    } catch (error) {
        console.error('Error:', error);
    } finally {
        // 隐藏等待图标
        loadingElement.style.display = 'none';
    }
}
