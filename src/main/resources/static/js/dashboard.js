document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initDashboard();
    initUpload();
    initAlerts();
    initTimeline();
    initRules();

    // Initial data fetch
    fetchAllData();

    // Auto refresh every 30 seconds
    setInterval(fetchAllData, 30000);
});

// --- State ---
let alertsData = [];
let uploadHistory = [];

// --- Navigation & Routing ---
function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('.content-section');

    function switchSection(targetId) {
        // Update active nav
        navItems.forEach(item => {
            if (item.dataset.target === targetId) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Update active section
        sections.forEach(sec => {
            if (sec.id === targetId) {
                sec.classList.add('active');
            } else {
                sec.classList.remove('active');
            }
        });
    }

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            // Let the hash change naturally
            const target = item.dataset.target;
            switchSection(target);
        });
    });

    // Handle initial hash
    const hash = window.location.hash;
    if (hash) {
        const targetSection = hash.substring(1) + '-section';
        const exists = document.getElementById(targetSection);
        if (exists) {
            switchSection(targetSection);
        }
    }
}

function fetchAllData() {
    fetchStats();
    fetchRecentFiles();
    fetchUploadHistory();
}

// --- Dashboard Section ---
async function fetchStats() {
    try {
        const res = await fetch('/api/stats').catch(() => null);
        let data;
        if (!res || !res.ok) {
            // Mock data if backend not ready
            data = {
                totalFiles: 142,
                totalEntries: 1250432,
                totalAlerts: 843,
                avgProcessingTimeMs: 1450,
                alertsBySeverity: { 'CRITICAL': 45, 'HIGH': 120, 'MEDIUM': 300, 'LOW': 378 },
                alertsByRule: { 'SQL Injection': 120, 'XSS Attempt': 85, 'Brute Force': 310, 'Malware Signature': 45, 'Path Traversal': 60 }
            };
        } else {
            data = await res.json();
        }

        animateCounter(document.getElementById('stat-total-files'), data.totalFiles);
        animateCounter(document.getElementById('stat-total-entries'), data.totalEntries);
        animateCounter(document.getElementById('stat-total-alerts'), data.totalAlerts);
        
        document.getElementById('stat-avg-time').textContent = formatDuration(data.avgProcessingTimeMs);

        drawSeverityDonut(data.alertsBySeverity);
        drawRuleBarChart(data.alertsByRule);

    } catch (err) {
        console.error('Error fetching stats:', err);
    }
}

async function fetchRecentFiles() {
    try {
        const res = await fetch('/api/logs').catch(() => null);
        let files;
        if (!res || !res.ok) {
            files = [
                { id: 1, filename: 'auth_server.log', fileSize: 10485760, totalLines: 45000, alertCount: 12, status: 'COMPLETED', uploadedAt: new Date().toISOString() },
                { id: 2, filename: 'nginx_access.log', fileSize: 4503204, totalLines: 20000, alertCount: 4, status: 'COMPLETED', uploadedAt: new Date().toISOString() },
                { id: 3, filename: 'syslog.1', fileSize: 204800, totalLines: 1500, alertCount: 0, status: 'PROCESSING', uploadedAt: new Date().toISOString() }
            ];
        } else {
            files = await res.json();
        }

        const tbody = document.getElementById('recent-files-tbody');
        tbody.innerHTML = '';
        
        files.slice(0, 5).forEach(f => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${f.filename}</td>
                <td>${formatBytes(f.fileSize)}</td>
                <td>${f.totalLines.toLocaleString()}</td>
                <td>${f.alertCount}</td>
                <td><span class="badge ${getStatusBadgeClass(f.status)}">${f.status}</span></td>
                <td>${formatDate(f.uploadedAt)}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error(err);
    }
}

function initDashboard() {
    // Handled by fetchAllData
}

// --- Canvas Charts ---
function drawSeverityDonut(dataObj) {
    const canvas = document.getElementById('severityDonutChart');
    if (!canvas) return;
    
    // Setup high DPI canvas
    const rect = canvas.parentElement.getBoundingClientRect();
    canvas.width = rect.width * 2;
    canvas.height = rect.height * 2;
    canvas.style.width = `${rect.width}px`;
    canvas.style.height = `${rect.height}px`;
    
    const ctx = canvas.getContext('2d');
    ctx.scale(2, 2);
    ctx.clearRect(0, 0, rect.width, rect.height);

    const colors = {
        'CRITICAL': '#d63031',
        'HIGH': '#fdcb6e',
        'MEDIUM': '#fd79a8',
        'LOW': '#0984e3'
    };

    let total = Object.values(dataObj).reduce((a, b) => a + b, 0);
    if(total === 0) total = 1;

    const centerX = rect.width / 2;
    const centerY = rect.height / 2;
    const radius = Math.min(centerX, centerY) * 0.7;
    const innerRadius = radius * 0.6;

    let startAngle = -Math.PI / 2;

    Object.entries(dataObj).forEach(([label, value]) => {
        if(value === 0) return;
        const sliceAngle = (value / total) * 2 * Math.PI;
        
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle);
        ctx.arc(centerX, centerY, innerRadius, startAngle + sliceAngle, startAngle, true);
        ctx.closePath();
        
        ctx.fillStyle = colors[label] || '#ffffff';
        ctx.fill();
        
        // Add tiny gap
        ctx.strokeStyle = '#12122a';
        ctx.lineWidth = 2;
        ctx.stroke();

        startAngle += sliceAngle;
    });

    // Center text
    ctx.fillStyle = '#f5f6fa';
    ctx.font = 'bold 24px Inter';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(total.toString(), centerX, centerY - 10);
    
    ctx.fillStyle = '#a4b0be';
    ctx.font = '12px Inter';
    ctx.fillText('Total', centerX, centerY + 15);
}

function drawRuleBarChart(dataObj) {
    const canvas = document.getElementById('ruleBarChart');
    if (!canvas) return;

    const rect = canvas.parentElement.getBoundingClientRect();
    canvas.width = rect.width * 2;
    canvas.height = rect.height * 2;
    canvas.style.width = `${rect.width}px`;
    canvas.style.height = `${rect.height}px`;
    
    const ctx = canvas.getContext('2d');
    ctx.scale(2, 2);
    ctx.clearRect(0, 0, rect.width, rect.height);

    const entries = Object.entries(dataObj).sort((a, b) => b[1] - a[1]).slice(0, 5);
    const maxVal = Math.max(...entries.map(e => e[1]), 1);
    
    const padding = 40;
    const barHeight = (rect.height - padding * 2) / 5 - 10;
    const maxBarWidth = rect.width - padding * 2 - 100;

    entries.forEach((entry, i) => {
        const [label, value] = entry;
        const y = padding + i * (barHeight + 10);
        const barWidth = (value / maxVal) * maxBarWidth;

        // Label
        ctx.fillStyle = '#f5f6fa';
        ctx.font = '12px Inter';
        ctx.textAlign = 'right';
        ctx.textBaseline = 'middle';
        
        // Truncate label if too long
        let shortLabel = label.length > 15 ? label.substring(0, 13) + '...' : label;
        ctx.fillText(shortLabel, padding + 90, y + barHeight / 2);

        // Bar background
        ctx.fillStyle = 'rgba(255,255,255,0.05)';
        ctx.beginPath();
        ctx.roundRect(padding + 100, y, maxBarWidth, barHeight, 4);
        ctx.fill();

        // Bar fill
        const gradient = ctx.createLinearGradient(padding + 100, 0, padding + 100 + barWidth, 0);
        gradient.addColorStop(0, '#6c5ce7');
        gradient.addColorStop(1, '#00cec9');
        
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.roundRect(padding + 100, y, barWidth || 1, barHeight, 4);
        ctx.fill();

        // Value
        ctx.fillStyle = '#a4b0be';
        ctx.textAlign = 'left';
        ctx.fillText(value.toString(), padding + 110 + barWidth, y + barHeight / 2);
    });
}

// --- Upload Section ---
function initUpload() {
    const dropzone = document.getElementById('log-dropzone');
    const fileInput = document.getElementById('file-input');

    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, preventDefaults, false);
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, () => dropzone.classList.add('dragover'), false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, () => dropzone.classList.remove('dragover'), false);
    });

    dropzone.addEventListener('drop', (e) => {
        let dt = e.dataTransfer;
        let files = dt.files;
        handleFiles(files);
    });

    fileInput.addEventListener('change', function() {
        handleFiles(this.files);
    });
}

function handleFiles(files) {
    if (files.length === 0) return;
    const file = files[0];
    uploadFile(file);
}

function uploadFile(file) {
    const progressDiv = document.getElementById('upload-progress');
    const progressFill = document.getElementById('progress-fill');
    const progressText = document.getElementById('progress-text');
    
    progressDiv.classList.remove('hidden');
    progressFill.style.width = '0%';
    progressText.innerText = `Uploading ${file.name}...`;

    const formData = new FormData();
    formData.append('file', file);

    // Simulate progress since fetch doesn't support upload progress natively easily
    let progress = 0;
    const simInterval = setInterval(() => {
        progress += 5;
        if(progress > 90) clearInterval(simInterval);
        progressFill.style.width = `${progress}%`;
    }, 100);

    fetch('/api/logs/upload', {
        method: 'POST',
        body: formData
    })
    .then(res => {
        if (!res.ok) throw new Error('Upload failed');
        return res.json();
    })
    .then(data => {
        clearInterval(simInterval);
        progressFill.style.width = '100%';
        progressText.innerText = 'Upload Complete! Processing...';
        showNotification('File uploaded successfully', 'success');
        
        setTimeout(() => {
            progressDiv.classList.add('hidden');
            fetchAllData(); // Refresh to show new file in processing
            pollFileStatus(data.id);
        }, 1500);
    })
    .catch(err => {
        clearInterval(simInterval);
        progressFill.style.background = 'var(--danger)';
        progressText.innerText = 'Upload failed.';
        showNotification('Failed to upload file', 'error');
        console.error(err);
    });
}

function pollFileStatus(id) {
    if(!id) return;
    const interval = setInterval(() => {
        fetch('/api/logs')
            .then(res => res.ok ? res.json() : [])
            .then(files => {
                const file = files.find(f => f.id === id);
                if(file && (file.status === 'COMPLETED' || file.status === 'FAILED')) {
                    clearInterval(interval);
                    fetchAllData();
                    showNotification(`Processing ${file.status.toLowerCase()} for ${file.filename}`, 
                        file.status === 'COMPLETED' ? 'success' : 'error');
                }
            })
            .catch(() => clearInterval(interval));
    }, 2000);
}

async function fetchUploadHistory() {
    try {
        const res = await fetch('/api/logs').catch(() => null);
        let files;
        if (!res || !res.ok) {
            files = [
                { id: 1, filename: 'auth_server.log', fileSize: 10485760, status: 'COMPLETED', processingTimeMs: 1200, uploadedAt: new Date().toISOString() }
            ];
        } else {
            files = await res.json();
        }
        
        uploadHistory = files;
        const tbody = document.getElementById('upload-history-tbody');
        tbody.innerHTML = '';
        
        files.forEach(f => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${f.filename}</td>
                <td>${formatBytes(f.fileSize)}</td>
                <td><span class="badge ${getStatusBadgeClass(f.status)}">${f.status}</span></td>
                <td>${f.processingTimeMs ? formatDuration(f.processingTimeMs) : '-'}</td>
                <td>${formatDate(f.uploadedAt)}</td>
            `;
            tbody.appendChild(tr);
        });
        
        updateFileFilterDropdowns(files);
    } catch (err) {
        console.error(err);
    }
}

function updateFileFilterDropdowns(files) {
    const timelineFilter = document.getElementById('timeline-file-filter');
    const currentVal = timelineFilter.value;
    
    let opts = '<option value="">All Files</option>';
    files.forEach(f => {
        opts += `<option value="${f.id}">${f.filename}</option>`;
    });
    
    timelineFilter.innerHTML = opts;
    timelineFilter.value = currentVal;
}

// --- Alerts Section ---
function initAlerts() {
    const sevFilter = document.getElementById('filter-severity');
    const ruleFilter = document.getElementById('filter-rule');
    const searchFilter = document.getElementById('filter-search');

    [sevFilter, ruleFilter].forEach(el => el.addEventListener('change', fetchFilteredAlerts));
    searchFilter.addEventListener('input', debounce(renderAlertsTable, 300));
    
    // Initial fetch
    fetchFilteredAlerts();
}

async function fetchFilteredAlerts() {
    const sev = document.getElementById('filter-severity').value;
    const rule = document.getElementById('filter-rule').value;
    
    let url = '/api/alerts?';
    if(sev) url += `severity=${sev}&`;
    if(rule) url += `rule=${rule}&`;

    try {
        const res = await fetch(url).catch(() => null);
        if (!res || !res.ok) {
            alertsData = [
                { id: 1, ruleName: 'SQL Injection', severity: 'CRITICAL', category: 'Web Attack', lineNumber: 405, matchedLine: '192.168.1.10 - - [24/Sep/2026:10:15:32 +0000] "GET /api/users?id=1%27%20OR%20%271%27=%271 HTTP/1.1" 200 453', timestamp: new Date().toISOString(), logFileId: 1 },
                { id: 2, ruleName: 'Brute Force', severity: 'HIGH', category: 'Authentication', lineNumber: 1024, matchedLine: 'Failed password for invalid user admin from 10.0.0.5 port 22 ssh2', timestamp: new Date().toISOString(), logFileId: 1 }
            ];
        } else {
            alertsData = await res.json();
        }
        
        // Populate rule filter options if empty
        const ruleFilter = document.getElementById('filter-rule');
        if(ruleFilter.options.length <= 1) {
            const uniqueRules = [...new Set(alertsData.map(a => a.ruleName))];
            uniqueRules.forEach(r => {
                const opt = document.createElement('option');
                opt.value = r;
                opt.textContent = r;
                ruleFilter.appendChild(opt);
            });
        }
        
        renderAlertsTable();
    } catch (err) {
        console.error(err);
    }
}

function renderAlertsTable() {
    const tbody = document.getElementById('alerts-tbody');
    const searchTerm = document.getElementById('filter-search').value.toLowerCase();
    tbody.innerHTML = '';
    
    const filtered = alertsData.filter(a => {
        if(!searchTerm) return true;
        return a.matchedLine.toLowerCase().includes(searchTerm) || 
               a.ruleName.toLowerCase().includes(searchTerm);
    });

    filtered.slice(0, 25).forEach(a => {
        const tr = document.createElement('tr');
        tr.className = 'expandable';
        tr.innerHTML = `
            <td>${formatDate(a.timestamp)}</td>
            <td><span class="badge badge-${a.severity.toLowerCase()}">${a.severity}</span></td>
            <td>${a.ruleName}</td>
            <td>${a.category}</td>
            <td>${a.lineNumber}</td>
            <td>#${a.logFileId}</td>
        `;
        
        const detailsTr = document.createElement('tr');
        const detailsTd = document.createElement('td');
        detailsTd.colSpan = 6;
        detailsTd.style.padding = 0;
        detailsTd.style.border = 'none';
        
        const detailsDiv = document.createElement('div');
        detailsDiv.className = 'expanded-row-details';
        detailsDiv.textContent = a.matchedLine;
        detailsTd.appendChild(detailsDiv);
        detailsTr.appendChild(detailsTd);

        tr.addEventListener('click', () => {
            detailsDiv.classList.toggle('show');
        });

        tbody.appendChild(tr);
        tbody.appendChild(detailsTr);
    });
}

// --- Timeline Section ---
function initTimeline() {
    const fileFilter = document.getElementById('timeline-file-filter');
    fileFilter.addEventListener('change', fetchTimeline);
    fetchTimeline();
}

async function fetchTimeline() {
    const fileId = document.getElementById('timeline-file-filter').value;
    const url = fileId ? `/api/timeline/${fileId}` : '/api/timeline';
    
    try {
        const res = await fetch(url).catch(() => null);
        let events;
        if (!res || !res.ok) {
            events = [
                { id: 1, eventType: 'ALERT', eventSource: 'Rule: SQL Injection', eventDetail: 'Detected malicious payload in URI', eventTimestamp: new Date().toISOString(), severity: 'CRITICAL', logFileId: 1 },
                { id: 2, eventType: 'FILE_UPLOAD', eventSource: 'System', eventDetail: 'Started processing auth_server.log', eventTimestamp: new Date(Date.now() - 3600000).toISOString(), severity: 'INFO', logFileId: 1 }
            ];
        } else {
            events = await res.json();
        }
        
        const container = document.getElementById('timeline-container');
        container.innerHTML = '';
        
        events.forEach(ev => {
            const el = document.createElement('div');
            el.className = 'timeline-event';
            
            const sevColor = getSeverityColor(ev.severity);
            
            el.innerHTML = `
                <div class="timeline-dot" style="background: ${sevColor}"></div>
                <div class="timeline-card">
                    <div class="timeline-time">${formatDate(ev.eventTimestamp)}</div>
                    <div class="timeline-title">
                        <span class="badge badge-${ev.severity.toLowerCase() || 'info'}">${ev.eventType}</span>
                        ${ev.eventSource}
                    </div>
                    <div class="timeline-detail">${ev.eventDetail}</div>
                </div>
            `;
            container.appendChild(el);
        });
    } catch (err) {
        console.error(err);
    }
}

// --- IOC Rules Section ---
function initRules() {
    // In a real app, might fetch from /api/rules
    const rules = [
        { name: "SQL Injection", category: "Web Attack", severity: "CRITICAL", pattern: "(?i)(union.*select|select.*from|drop\\s+table|1=1|'\\s*OR\\s*'1'='1)" },
        { name: "XSS Attempt", category: "Web Attack", severity: "HIGH", pattern: "(?i)(<script>|javascript:|onerror=|onload=)" },
        { name: "Path Traversal", category: "Web Attack", severity: "HIGH", pattern: "(?i)(\\.\\./\\.\\./|%2e%2e%2f)" },
        { name: "Command Injection", category: "System", severity: "CRITICAL", pattern: "(?i)(;|\\||&&)\\s*(bash|sh|cmd|powershell|nc|netcat)" },
        { name: "Brute Force", category: "Authentication", severity: "MEDIUM", pattern: "(?i)(failed\\s+password|login\\s+failed|invalid\\s+user).*{5,}" },
        { name: "Malware Signature", category: "Malware", severity: "CRITICAL", pattern: "(?i)(miner|stratum|xmrig|mimikatz)" }
    ];
    
    const grid = document.getElementById('rules-grid');
    grid.innerHTML = '';
    
    rules.forEach(r => {
        const card = document.createElement('div');
        card.className = 'rule-card';
        card.innerHTML = `
            <div class="rule-header">
                <div class="rule-name">${r.name}</div>
                <span class="badge badge-${r.severity.toLowerCase()}">${r.severity}</span>
            </div>
            <div>
                <span class="badge" style="background: rgba(255,255,255,0.1); font-size: 10px;">${r.category}</span>
            </div>
            <div class="rule-pattern">${r.pattern.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</div>
        `;
        grid.appendChild(card);
    });
}

// --- Utilities ---
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(isoString) {
    if(!isoString) return '-';
    const d = new Date(isoString);
    return d.toLocaleString();
}

function formatDuration(ms) {
    if(!ms) return '0ms';
    if(ms < 1000) return `${ms}ms`;
    return `${(ms/1000).toFixed(2)}s`;
}

function getStatusBadgeClass(status) {
    switch(status?.toUpperCase()) {
        case 'COMPLETED': return 'badge-success';
        case 'PROCESSING': return 'badge-processing';
        case 'FAILED': return 'badge-critical';
        default: return 'badge-low';
    }
}

function getSeverityColor(severity) {
    switch(severity?.toUpperCase()) {
        case 'CRITICAL': return 'var(--danger)';
        case 'HIGH': return 'var(--warning)';
        case 'MEDIUM': return 'var(--accent)';
        case 'LOW': return 'var(--info)';
        default: return 'var(--primary)';
    }
}

function animateCounter(el, target, duration = 1000) {
    if(!el) return;
    const start = parseInt(el.innerText.replace(/,/g, '')) || 0;
    const diff = target - start;
    if(diff === 0) {
        el.innerText = target.toLocaleString();
        return;
    }
    
    const startTime = performance.now();
    
    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // easeOutQuart
        const ease = 1 - Math.pow(1 - progress, 4);
        const current = Math.floor(start + (diff * ease));
        
        el.innerText = current.toLocaleString();
        
        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            el.innerText = target.toLocaleString();
        }
    }
    
    requestAnimationFrame(update);
}

function showNotification(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'ℹ️';
    if(type === 'success') icon = '✅';
    if(type === 'error') icon = '❌';
    
    toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
    container.appendChild(toast);
    
    // Trigger reflow
    toast.offsetHeight;
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
