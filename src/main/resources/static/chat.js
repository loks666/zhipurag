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
    loadingElement.innerText = '思考中……';

    try {
        const response = await fetch('/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ user: message, requestId: currentRequestId })
        });

        const result = await response.json();
        currentRequestId = result.requestId; // 保存返回的 requestId
        appendMessage(result.system, 'ai');
    } catch (error) {
        console.error('Error:', error);
    } finally {
        // 隐藏等待图标
        loadingElement.style.display = 'none';
    }
}
