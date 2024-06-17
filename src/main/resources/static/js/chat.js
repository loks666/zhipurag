let currentRequestId = null;

function newChat() {
    document.getElementById('messages').innerHTML = '';
    currentRequestId = null; // 清除当前的 requestId
}

function appendMessage(content, sender, isMarkdown = false) {
    const messageContainer = document.createElement('div');
    messageContainer.className = `message ${sender}`;
    const messageBubble = document.createElement('div');
    messageBubble.className = 'message-bubble';

    if (isMarkdown) {
        messageBubble.innerHTML = marked.parse(content); // 使用 marked.js 渲染 Markdown 内容
    } else {
        messageBubble.innerHTML = content.replace(/\n/g, '<br>');
    }

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
    loadingElement.style.display = 'inline';

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

        if (!response.ok) {
            return Promise.reject(new Error(`HTTP error! status: ${response.status}`));
        }

        const result = await response.json();
        console.log('Response:', result); // 添加日志输出以调试问题
        currentRequestId = result.requestId; // 更新 requestId
        appendMessage(result.system, 'ai', true); // 假设系统消息包含Markdown
    } catch (error) {
        console.error('Error:', error);
        appendMessage(`Error: ${error.message}`, 'system'); // 显示错误消息
    } finally {
        // 隐藏等待图标
        loadingElement.style.display = 'none';
    }
}
