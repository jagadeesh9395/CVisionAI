document.addEventListener('DOMContentLoaded', function() {
    // DOM Elements
    const analyzeBtn = document.getElementById('analyzeBtn');
    const copyBtn = document.getElementById('copyBtn');
    const inputText = document.getElementById('inputText');
    const jsonSchema = document.getElementById('jsonSchema');
    const analyzeText = document.getElementById('analyzeText');
    const analyzeSpinner = document.getElementById('analyzeSpinner');

    // Event Listeners
    analyzeBtn.addEventListener('click', analyzeTextHandler);
    copyBtn.addEventListener('click', copyResultsToClipboard);
    
    // Handle Enter key in textareas
    [inputText, jsonSchema].forEach(textarea => {
        textarea.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && e.ctrlKey) {
                e.preventDefault();
                analyzeTextHandler();
            }
        });
    });

    // Analyze Text Handler
    async function analyzeTextHandler() {
        const text = inputText.value.trim();
        const schema = jsonSchema.value.trim();

        if (!text) {
            showAlert('Please enter some text to analyze.', 'warning');
            inputText.focus();
            return;
        }

        if (!schema) {
            showAlert('Please provide a JSON schema for the output format.', 'warning');
            jsonSchema.focus();
            return;
        }

        try {
            // Validate JSON schema
            JSON.parse(schema);
        } catch (e) {
            showAlert('Invalid JSON schema. Please check your format.', 'danger');
            return;
        }

        // Show loading state
        setLoadingState(true);
        document.getElementById('loadingIndicator').classList.remove('d-none');
        document.getElementById('resultsContainer').innerHTML = '';

        try {
            const response = await fetch('/api/analyze', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    text: text,
                    schema: schema
                })
            });

            if (!response.ok) {
                throw new Error(`Error: ${response.status}`);
            }

            const data = await response.json();
            displayResults(data);
            
        } catch (error) {
            console.error('Error:', error);
            showResultsError('Error analyzing text. Please try again.');
        } finally {
            setLoadingState(false);
            document.getElementById('loadingIndicator').classList.add('d-none');
        }
    }

    function displayResults(data) {
        const container = document.getElementById('resultsContainer');
        
        // Clear previous results
        container.innerHTML = '';

        // Create result cards based on the data structure
        if (data.summary) {
            container.appendChild(createResultCard('Summary', data.summary, 'primary'));
        }

        if (data.keyPoints && data.keyPoints.length > 0) {
            container.appendChild(createListCard('Key Points', data.keyPoints, 'success'));
        }

        if (data.sentiment) {
            const sentimentHtml = `
                <div class="d-flex align-items-center mb-2">
                    <span class="me-2">Score:</span>
                    <div class="progress flex-grow-1" style="height: 20px;">
                        <div class="progress-bar" role="progressbar" 
                             style="width: ${(data.sentiment.score + 1) * 50}%" 
                             aria-valuenow="${data.sentiment.score}" 
                             aria-valuemin="-1" 
                             aria-valuemax="1">
                            ${data.sentiment.score.toFixed(2)}
                        </div>
                    </div>
                </div>
                <div>Label: <span class="badge bg-${getSentimentBadgeClass(data.sentiment.label)}">
                    ${data.sentiment.label}
                </span></div>
            `;
            container.appendChild(createResultCard('Sentiment Analysis', sentimentHtml, 'info'));
        }

        if (data.entities && data.entities.length > 0) {
            const entitiesHtml = data.entities.map(entity => `
                <div class="mb-2">
                    <span class="badge bg-secondary me-2">${entity.type}</span>
                    ${entity.value}
                </div>
            `).join('');
            container.appendChild(createResultCard('Entities', entitiesHtml, 'warning'));
        }

        // If no specific data structure, show raw JSON
        if (container.children.length === 0) {
            container.appendChild(createResultCard('Analysis Results', 
                `<pre class="bg-light p-3 rounded">${JSON.stringify(data, null, 2)}</pre>`, 
                'secondary'));
        }
    }

    function createResultCard(title, content, type = 'primary') {
        const card = document.createElement('div');
        card.className = `card border-${type} mb-3`;
        card.innerHTML = `
            <div class="card-header bg-${type} bg-opacity-10 text-${type}">
                <h6 class="mb-0">${title}</h6>
            </div>
            <div class="card-body">
                ${typeof content === 'string' ? content : JSON.stringify(content, null, 2)}
            </div>
        `;
        return card;
    }

    function createListCard(title, items, type = 'primary') {
        const listItems = items.map(item => `<li class="list-group-item">${item}</li>`).join('');
        const card = document.createElement('div');
        card.className = `card border-${type} mb-3`;
        card.innerHTML = `
            <div class="card-header bg-${type} bg-opacity-10 text-${type}">
                <h6 class="mb-0">${title}</h6>
            </div>
            <ul class="list-group list-group-flush">
                ${listItems}
            </ul>
        `;
        return card;
    }

    function getSentimentBadgeClass(label) {
        if (!label) return 'secondary';
        const lowerLabel = label.toLowerCase();
        if (lowerLabel.includes('positive')) return 'success';
        if (lowerLabel.includes('negative')) return 'danger';
        if (lowerLabel.includes('neutral')) return 'secondary';
        return 'primary';
    }

    function showResultsError(message) {
        const container = document.getElementById('resultsContainer');
        container.innerHTML = `
            <div class="alert alert-danger d-flex align-items-center" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                <div>${message}</div>
            </div>
        `;
    }

    // Copy Results to Clipboard
    function copyResultsToClipboard() {
        const resultsContainer = document.getElementById('resultsContainer');
        if (!resultsContainer || !resultsContainer.textContent.trim()) {
            showAlert('No results to copy!', 'warning');
            return;
        }
        
        navigator.clipboard.writeText(resultsContainer.innerText)
            .then(() => showAlert('Results copied to clipboard!', 'success'))
            .catch(() => showAlert('Failed to copy results', 'danger'));
    }

    // Helper Functions
    function setLoadingState(isLoading) {
        if (isLoading) {
            analyzeText.textContent = 'Analyzing...';
            analyzeSpinner.classList.remove('d-none');
            analyzeBtn.disabled = true;
        } else {
            analyzeText.textContent = 'Analyze Text';
            analyzeSpinner.classList.add('d-none');
            analyzeBtn.disabled = false;
        }
    }

    function showAlert(message, type) {
        // Remove any existing alerts
        const existingAlert = document.querySelector('.alert');
        if (existingAlert) {
            existingAlert.remove();
        }

        // Create alert
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.role = 'alert';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;

        // Add to DOM
        document.querySelector('.container-fluid').prepend(alertDiv);

        // Auto-dismiss after 5 seconds
        setTimeout(() => {
            const alert = bootstrap.Alert.getInstance(alertDiv);
            if (alert) alert.close();
        }, 5000);
    }
});
