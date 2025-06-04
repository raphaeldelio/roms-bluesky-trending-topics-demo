// Function to save toggle state to localStorage
function saveToggleState(targetId, isVisible) {
    localStorage.setItem(`toggle_${targetId}`, isVisible ? 'visible' : 'hidden');
}

// Function to load toggle state from localStorage
function loadToggleState(targetId) {
    return localStorage.getItem(`toggle_${targetId}`);
}

// Function to fetch Redis keys by prefix
function fetchKeysByPrefix(prefix) {
    console.log(`Fetching keys with prefix: ${prefix}`);
    return fetch(`/api/keys?prefix=${prefix}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            console.log(`Received response for prefix ${prefix}`);
            return response.json();
        })
        .then(data => {
            // Log the raw data for debugging
            console.log(`Data for prefix ${prefix}:`, data);

            // Ensure we're returning an object that can be processed with Object.entries()
            if (!data) {
                console.error(`No data returned for prefix ${prefix}`);
                return {};
            }

            if (typeof data !== 'object') {
                console.error(`Data is not an object for prefix ${prefix}:`, data);
                return {};
            }

            if (Array.isArray(data)) {
                console.error(`Data is an array for prefix ${prefix}:`, data);
                // Convert array to object if possible
                if (data.length > 0) {
                    const obj = {};
                    data.forEach((item, index) => {
                        if (typeof item === 'object' && item !== null) {
                            // If array contains objects with key/value pairs
                            Object.entries(item).forEach(([k, v]) => {
                                obj[k] = v;
                            });
                        } else {
                            // If array contains simple values
                            obj[`item${index}`] = item;
                        }
                    });
                    console.log(`Converted array to object for prefix ${prefix}:`, obj);
                    return obj;
                }
                return {};
            }

            // Check if the object is empty
            if (Object.keys(data).length === 0) {
                console.log(`No keys found for prefix ${prefix}`);
            } else {
                console.log(`Found ${Object.keys(data).length} keys for prefix ${prefix}`);
            }

            return data;
        })
        .catch(error => {
            console.error(`Error fetching keys for prefix ${prefix}:`, error);
            return {};
        });
}

// Function to fetch data for a specific key
function fetchDataForKey(key, dataType) {
    console.log(`Fetching data for key: ${key}, type: ${dataType}`);
    return fetch(`/api/data?key=${key}&type=${dataType}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            console.log(`Received response for key ${key}, type ${dataType}`);
            return response.json();
        })
        .then(data => {
            // Log the raw data for debugging
            console.log(`Data for key ${key}, type ${dataType}:`, data);

            // Handle null or undefined data
            if (!data) {
                console.error(`No data returned for key ${key}, type ${dataType}`);
                return {};
            }

            return data;
        })
        .catch(error => {
            console.error(`Error fetching data for key ${key}, type ${dataType}:`, error);
            return {};
        });
}

// Function to check if an item exists in a Bloom Filter
function checkBloomFilter(key, item) {
    console.log(`Checking if item "${item}" exists in Bloom Filter "${key}"`);
    return fetch(`/api/bloom/check?key=${key}&item=${encodeURIComponent(item)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            console.log(`Received response for Bloom Filter check: ${key}, item: ${item}`);
            return response.json();
        })
        .then(data => {
            console.log(`Bloom Filter check result for key ${key}, item ${item}:`, data);
            return data;
        })
        .catch(error => {
            console.error(`Error checking Bloom Filter ${key} for item ${item}:`, error);
            return { error: error.message };
        });
}

function populateSelect(selectId, keySizeMap) {
    console.log(`Populating select ${selectId} with data:`, keySizeMap);
    const select = document.getElementById(selectId);
    if (!select) {
        console.error(`Select element with ID ${selectId} not found`);
        return;
    }

    select.innerHTML = '';

    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = '-- Select a key --';
    select.appendChild(defaultOption);

    // Ensure keySizeMap is an object and not null or undefined
    if (keySizeMap && typeof keySizeMap === 'object' && !Array.isArray(keySizeMap)) {
        const entries = Object.entries(keySizeMap);

        if (entries.length === 0) {
            // No keys found, add a disabled option to indicate this
            const noKeysOption = document.createElement('option');
            noKeysOption.disabled = true;
            noKeysOption.textContent = 'No keys found';
            select.appendChild(noKeysOption);
            console.log(`No keys found for select ${selectId}`);
        } else {
            // Add options for each key
            entries.forEach(([key, size]) => {
                const optionElement = document.createElement('option');
                optionElement.value = key;
                optionElement.textContent = `${key} (${size})`;
                select.appendChild(optionElement);
            });
            console.log(`Added ${entries.length} options to select ${selectId}`);
        }
    } else {
        // Invalid keySizeMap, add a disabled option to indicate this
        const invalidOption = document.createElement('option');
        invalidOption.disabled = true;
        invalidOption.textContent = 'Error loading keys';
        select.appendChild(invalidOption);
        console.error(`Invalid keySizeMap for select ${selectId}:`, keySizeMap);
    }
}

// Function to display data in a container
function displayData(containerId, data) {
    console.log(`Displaying data in container ${containerId}:`, data);
    const container = document.getElementById(containerId);

    if (!container) {
        console.error(`Container element with ID ${containerId} not found`);
        return;
    }

    if (!data) {
        console.warn(`No data provided for container ${containerId}`);
        container.innerHTML = '<p class="error-message">No data available</p>';
        return;
    }

    if (Array.isArray(data) && data.length === 0) {
        console.warn(`Empty array provided for container ${containerId}`);
        container.innerHTML = '<p class="info-message">No items in this collection</p>';
        return;
    }

    if (typeof data === 'object' && !Array.isArray(data) && Object.keys(data).length === 0) {
        console.warn(`Empty object provided for container ${containerId}`);
        container.innerHTML = '<p class="info-message">No properties in this object</p>';
        return;
    }

    let html = '<ul class="data-list">';

    try {
        if (Array.isArray(data)) {
            // For arrays (like set members)
            data.forEach(item => {
                html += `<li>${typeof item === 'object' ? JSON.stringify(item) : item}</li>`;
            });
            console.log(`Displayed ${data.length} array items in container ${containerId}`);
        } else if (typeof data === 'object') {
            // For objects (like sorted set with scores)
            Object.entries(data).forEach(([key, value]) => {
                html += `<li><strong>${key}:</strong> ${typeof value === 'object' ? JSON.stringify(value) : value}</li>`;
            });
            console.log(`Displayed ${Object.keys(data).length} object properties in container ${containerId}`);
        } else {
            // For simple values
            html += `<li>${data}</li>`;
            console.log(`Displayed simple value in container ${containerId}`);
        }

        html += '</ul>';
        container.innerHTML = html;
    } catch (error) {
        console.error(`Error displaying data in container ${containerId}:`, error);
        container.innerHTML = '<p class="error-message">Error displaying data</p>';
    }
}

// Apply toggle states on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log("DOM content loaded, initializing toggle states");

    // Set up toggle buttons
    document.querySelectorAll(".toggle-btn").forEach(button => {
        const targetId = button.getAttribute("data-target");
        const targetElements = document.querySelectorAll(`[id^="${targetId}"]`);
        const savedState = loadToggleState(targetId);

        console.log(`Initializing toggle state for ${targetId}, saved state: ${savedState}`);

        // If there's a saved state in localStorage, use it
        if (savedState === 'visible') {
            console.log(`Setting ${targetId} to visible based on saved state`);
            // Apply to existing elements
            targetElements.forEach(targetElement => {
                targetElement.style.display = "block";
                console.log(`Set display for ${targetElement.id} to block`);
            });
            // Always update button state
            button.classList.add('active');

            // Refresh the select elements for this column
            if (targetId === 'countmin-column') {
                console.log(`Refreshing countmin selects`);
                fetchKeysByPrefix('words-bucket-cms:').then(keyMap => {
                    populateSelect('countmin-select', keyMap);
                });
                fetchKeysByPrefix('words-bucket-zset:').then(keyMap => {
                    populateSelect('countmin-sortedset-select', keyMap);
                });
            } else if (targetId === 'bloom-column') {
                console.log(`Refreshing bloom selects`);
                fetchKeysByPrefix('*bf').then(keyMap => {
                    populateSelect('bloom-select', keyMap);
                });
                fetchKeysByPrefix('*-set').then(keyMap => {
                    populateSelect('bloom-set-select', keyMap);
                });
            } else if (targetId === 'topk-column') {
                console.log(`Refreshing topk selects`);
                fetchKeysByPrefix('spiking-topk:').then(keyMap => {
                    populateSelect('topk-select', keyMap);
                });
                fetchKeysByPrefix('spiking-zset:').then(keyMap => {
                    populateSelect('topk-sortedset-select', keyMap);
                });
            }
        } else if (savedState === 'hidden') {
            console.log(`Setting ${targetId} to hidden based on saved state`);
            // Apply to existing elements
            targetElements.forEach(targetElement => {
                targetElement.style.display = "none";
                console.log(`Set display for ${targetElement.id} to none`);
            });
            // Always update button state
            button.classList.remove('active');
        } else {
            console.log(`No saved state for ${targetId}, initializing based on current display state`);
            // No saved state, initialize based on current display state
            // Only check elements if they exist
            if (targetElements.length > 0) {
                // Check if any of the target elements are visible
                let isAnyVisible = false;
                targetElements.forEach(targetElement => {
                    // Get computed style to check actual display value
                    const computedStyle = window.getComputedStyle(targetElement);
                    if (computedStyle.display !== 'none') {
                        isAnyVisible = true;
                    }
                    console.log(`Current display for ${targetElement.id}: ${computedStyle.display}`);
                });

                // Save initial state to localStorage
                saveToggleState(targetId, isAnyVisible);
                console.log(`Saved initial state for ${targetId}: ${isAnyVisible}`);

                // Set button active state based on visibility
                if (isAnyVisible) {
                    button.classList.add('active');
                    console.log(`Set button for ${targetId} to active`);

                    // Refresh the select elements for this column
                    if (targetId === 'countmin-column') {
                        console.log(`Refreshing countmin selects`);
                        fetchKeysByPrefix('words-bucket-cms:').then(keyMap => {
                            populateSelect('countmin-select', keyMap);
                        });
                        fetchKeysByPrefix('words-bucket-zset:').then(keyMap => {
                            populateSelect('countmin-sortedset-select', keyMap);
                        });
                    } else if (targetId === 'bloom-column') {
                        console.log(`Refreshing bloom selects`);
                        fetchKeysByPrefix('*bf').then(keyMap => {
                            populateSelect('bloom-select', keyMap);
                        });
                        fetchKeysByPrefix('*-set').then(keyMap => {
                            populateSelect('bloom-set-select', keyMap);
                        });
                    } else if (targetId === 'topk-column') {
                        console.log(`Refreshing topk selects`);
                        fetchKeysByPrefix('spiking-topk:').then(keyMap => {
                            populateSelect('topk-select', keyMap);
                        });
                        fetchKeysByPrefix('spiking-zset:').then(keyMap => {
                            populateSelect('topk-sortedset-select', keyMap);
                        });
                    }
                } else {
                    button.classList.remove('active');
                    console.log(`Set button for ${targetId} to inactive`);
                }
            } else {
                // Initialize as hidden/inactive
                saveToggleState(targetId, false);
                button.classList.remove('active');
                console.log(`No elements found for ${targetId}, initializing as hidden/inactive`);
            }
        }
    });

    // Set up Count-Min Sketch selects
    fetchKeysByPrefix('words-bucket-cms:').then(keyMap => {
        populateSelect('countmin-select', keyMap);
    });

    fetchKeysByPrefix('words-bucket-zset:').then(keyMap => {
        populateSelect('countmin-sortedset-select', keyMap);
    });

    // Set up Bloom Filter selects
    fetchKeysByPrefix('*bf').then(keyMap => {
        populateSelect('bloom-select', keyMap);
    });

    fetchKeysByPrefix('*-set').then(keyMap => {
        populateSelect('bloom-set-select', keyMap);
    });

    // Set up TopK selects
    fetchKeysByPrefix('spiking-topk:').then(keyMap => {
        populateSelect('topk-select', keyMap);
    });

    fetchKeysByPrefix('spiking-zset:').then(keyMap => {
        populateSelect('topk-sortedset-select', keyMap);
    });

    // Set up change listeners for selects
    document.getElementById('countmin-select').addEventListener('change', function() {
        if (this.value) {
            fetchDataForKey(this.value, 'cms').then(data => {
                displayData('countmin-data', data);
            });
        } else {
            document.getElementById('countmin-data').innerHTML = '';
        }
    });

    document.getElementById('countmin-sortedset-select').addEventListener('change', function() {
        if (this.value) {
            fetchDataForKey(this.value, 'zset').then(data => {
                displayData('countmin-sortedset-data', data);
            });
        } else {
            document.getElementById('countmin-sortedset-data').innerHTML = '';
        }
    });

    document.getElementById('bloom-select').addEventListener('change', function() {
        if (this.value) {
            fetchDataForKey(this.value, 'bf').then(data => {
                displayData('bloom-data', data);
            });
        } else {
            document.getElementById('bloom-data').innerHTML = '';
        }
    });

    document.getElementById('bloom-set-select').addEventListener('change', function() {
        if (this.value) {
            fetchDataForKey(this.value, 'set').then(data => {
                displayData('bloom-set-data', data);
            });
        } else {
            document.getElementById('bloom-set-data').innerHTML = '';
        }
    });

    document.getElementById('topk-select').addEventListener('change', function() {
        if (this.value) {
            fetchDataForKey(this.value, 'topk').then(data => {
                displayData('topk-data', data);
            });
        } else {
            document.getElementById('topk-data').innerHTML = '';
        }
    });

    document.getElementById('topk-sortedset-select').addEventListener('change', function() {
        if (this.value) {
            fetchDataForKey(this.value, 'zset').then(data => {
                displayData('topk-sortedset-data', data);
            });
        } else {
            document.getElementById('topk-sortedset-data').innerHTML = '';
        }
    });

    // Set up Bloom Filter check button
    document.getElementById('bloom-check-button').addEventListener('click', function() {
        const bloomFilter = document.getElementById('bloom-select').value;
        const itemToCheck = document.getElementById('bloom-check-input').value.trim();
        const resultContainer = document.getElementById('bloom-check-result');

        if (!bloomFilter) {
            resultContainer.innerHTML = '<p class="error-message">Please select a Bloom Filter first</p>';
            return;
        }

        if (!itemToCheck) {
            resultContainer.innerHTML = '<p class="error-message">Please enter an item to check</p>';
            return;
        }

        resultContainer.innerHTML = '<p>Checking...</p>';

        checkBloomFilter(bloomFilter, itemToCheck)
            .then(result => {
                if (result.error) {
                    resultContainer.innerHTML = `<p class="error-message">Error: ${result.error}</p>`;
                } else if (result.exists === true) {
                    resultContainer.innerHTML = `<p class="result-found">Item "${itemToCheck}" might exist in the Bloom Filter</p>`;
                } else if (result.exists === false) {
                    resultContainer.innerHTML = `<p class="result-not-found">Item "${itemToCheck}" definitely does not exist in the Bloom Filter</p>`;
                } else {
                    resultContainer.innerHTML = `<p class="info-message">Unexpected result: ${JSON.stringify(result)}</p>`;
                }
            })
            .catch(error => {
                resultContainer.innerHTML = `<p class="error-message">Error: ${error.message}</p>`;
            });
    });
});

document.querySelectorAll(".toggle-btn").forEach(button => {
    button.addEventListener("click", function() {
        const targetId = this.getAttribute("data-target");
        const targetElements = document.querySelectorAll(`[id^="${targetId}"]`);
        let isVisible = false;

        console.log(`Toggle button clicked for ${targetId}`);

        // If there are elements to toggle, toggle them
        if (targetElements.length > 0) {
            // Check current visibility state of first element
            const firstElement = targetElements[0];
            const computedStyle = window.getComputedStyle(firstElement);
            isVisible = computedStyle.display === "none";

            console.log(`Current display state for ${targetId}: ${computedStyle.display}`);

            // Toggle all matching elements
            targetElements.forEach(targetElement => {
                targetElement.style.display = isVisible ? "block" : "none";
                console.log(`Set display for ${targetElement.id} to ${isVisible ? "block" : "none"}`);
            });
        } else {
            // If there are no elements to toggle,
            // get the current state from the button's active class
            isVisible = !this.classList.contains('active');
            console.log(`No elements found for ${targetId}, using button state: ${isVisible}`);
        }

        // Toggle active class on button
        if (isVisible) {
            this.classList.add('active');
        } else {
            this.classList.remove('active');
        }

        console.log(`Button active state for ${targetId}: ${isVisible}`);

        // Save state to localStorage
        saveToggleState(targetId, isVisible);
        console.log(`Saved toggle state for ${targetId}: ${isVisible}`);

        // Refresh the select elements if they're now visible
        if (isVisible) {
            if (targetId === 'countmin-column') {
                fetchKeysByPrefix('words-bucket-cms:').then(keyMap => {
                    populateSelect('countmin-select', keyMap);
                });
                fetchKeysByPrefix('words-bucket-zset:').then(keyMap => {
                    populateSelect('countmin-sortedset-select', keyMap);
                });
            } else if (targetId === 'bloom-column') {
                fetchKeysByPrefix('*bf').then(keyMap => {
                    populateSelect('bloom-select', keyMap);
                });
                fetchKeysByPrefix('*-set').then(keyMap => {
                    populateSelect('bloom-set-select', keyMap);
                });
            } else if (targetId === 'topk-column') {
                fetchKeysByPrefix('spiking-topk:').then(keyMap => {
                    populateSelect('topk-select', keyMap);
                });
                fetchKeysByPrefix('spiking-zset:').then(keyMap => {
                    populateSelect('topk-sortedset-select', keyMap);
                });
            }
        }
    });
});

document.getElementById("query-form").addEventListener("submit", function(event) {
    event.preventDefault();

    let queryText = document.getElementById("query-input").value;
    let imageFile = document.getElementById("image-input").files[0];

    if (imageFile) {
        let reader = new FileReader();
        reader.readAsDataURL(imageFile);
        reader.onload = function() {
            let imageBase64 = reader.result.split(",")[1];
            sendQuery(queryText, imageBase64);
        };
        reader.onerror = function(error) {
            console.error("Error converting image:", error);
        };
    } else {
        sendQuery(queryText, null);
    }
});


// Function to check if a toggle is enabled
function isToggleEnabled(targetId) {
    const savedState = loadToggleState(targetId);
    return savedState === 'visible';
}

function sendQuery(query, imageBase64) {
    let requestBody = { query: query };
    if (imageBase64) requestBody.imageBase64 = imageBase64;

    // Create arrays to hold our fetch promises and their results
    const fetchPromises = [];

    // Only send requests for enabled search types
    if (isToggleEnabled('texts-response-column')) {
        fetchPromises.push(
            fetch("/utterance/search", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(requestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('questions-response-column')) {
        // Add enableRag and enableSemanticCache properties for question search
        const questionRequestBody = { ...requestBody };
        questionRequestBody.enableRag = isToggleEnabled('rag');
        questionRequestBody.enableSemanticCache = isToggleEnabled('semantic-cache');

        fetchPromises.push(
            fetch("/question/search/", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(questionRequestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('summaries-response-column')) {
        // Add enableRag and enableSemanticCache properties for summary search
        const summaryRequestBody = { ...requestBody };
        summaryRequestBody.enableRag = isToggleEnabled('rag');
        summaryRequestBody.enableSemanticCache = isToggleEnabled('semantic-cache');

        fetchPromises.push(
            fetch("/summary/search", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(summaryRequestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('images-text-response-column')) {
        fetchPromises.push(
            fetch("/image/search/by-description", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(requestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    if (isToggleEnabled('images-response-column')) {
        fetchPromises.push(
            fetch("/image/search/by-image", {
                method: "POST",
                headers: { "Content-Type": "application/json", "HX-Request": "true" },
                body: JSON.stringify(requestBody)
            }).then(res => res.json())
        );
    } else {
        fetchPromises.push(Promise.resolve(null));
    }

    Promise.all(fetchPromises).then(([textData, questionData, summaryData, imageTextData, imageData]) => {
        console.log("Text Response:", textData);
        console.log("Question Response:", questionData);
        console.log("Summary Response:", summaryData);
        console.log("Image Response:", imageData);
        console.log("Image Text Response:", imageTextData);

        // Render text-based response if enabled
        if (textData) {
            let textsHtml = `<p><strong>Embedding time:</strong> ${textData.embeddingTime}</p>`

            if (textData.searchTime) {
                textsHtml += `<p><strong>Search time:</strong> ${textData.searchTime}</p>`;
            }

            textsHtml += `<p><strong>Q:</strong> ${textData.query}</p>`;

            if (textData.matchedTexts && textData.matchedTexts.length > 0) {
                textsHtml += `<h4>Matched Texts:</h4><ul>`;
                textsHtml += textData.matchedTexts.map(q => `
                    <li>
                       ${q.utterance} (Score: ${parseFloat(q.score).toFixed(2)})
                    </li>
                    `).join("");
                textsHtml += `</ul>`;
            }

            document.getElementById("texts-response").innerHTML = textsHtml;
        }

        // Render question-based response if enabled
        if (questionData) {
            let questionsHTML = `<p><strong>Embedding time:</strong> ${questionData.embeddingTime}</p>`

            if (questionData.ragTime) {
                questionsHTML += `<p><strong>RAG time:</strong> ${questionData.ragTime}</p>`;
            }

            if (questionData.searchTime) {
                questionsHTML += `<p><strong>Search time:</strong> ${questionData.searchTime}</p>`;
            }

            if (questionData.cacheSearchTime) {
                questionsHTML += `<p><strong>Cache Search time:</strong> ${questionData.cacheSearchTime}</p>`;
            }

            questionsHTML += `<p><strong>Q:</strong> ${questionData.query}</p>`;

            if (isToggleEnabled('rag')) {
                questionsHTML += `<p><strong>A:</strong> ${questionData.ragAnswer}</p>`;
            }

            if (isToggleEnabled('semantic-cache')) {
                questionsHTML += `
                    <div>
                    <p><strong>Cached Query: </strong>${questionData.cachedQuery}</p>
                    <p><strong>Cached Score: </strong>Cached Score: ${questionData.cachedScore}</p>
                    </div>`;
            }

            if (questionData.matchedQuestions && questionData.matchedQuestions.length > 0) {
                questionsHTML += `<h4>Matched Questions:</h4><ul>`;
                questionsHTML += questionData.matchedQuestions.map(q => `
                        <li>
                            <button class="collapsible">${q.question} (Score: ${parseFloat(q.score).toFixed(2)})</button>
                            <div class="content">
                                <p><strong>Related Utterances:</strong></p>
                                <ul>
                                    ${q.utterances.split("\n").map(utterance => `<li>${utterance}</li>`).join("")}
                                </ul>
                            </div>
                        </li>
                        `).join("");
                questionsHTML += `</ul>`;
            }

            document.getElementById("questions-response").innerHTML = questionsHTML;
        }

        // Render summary-based response if enabled
        if (summaryData) {
            let summariesHTML = `<p><strong>Embedding time:</strong> ${summaryData.embeddingTime}</p>`

            if (summaryData.ragTime) {
                summariesHTML += `<p><strong>RAG time:</strong> ${summaryData.ragTime}</p>`;
            }

            if (summaryData.searchTime) {
                summariesHTML += `<p><strong>Search time:</strong> ${summaryData.searchTime}</p>`;
            }

            if (summaryData.cacheSearchTime) {
                summariesHTML += `<p><strong>Cache Search time:</strong> ${summaryData.cacheSearchTime}</p>`;
            }

            summariesHTML += `<p><strong>Q:</strong> ${summaryData.query}</p>`;

            if (isToggleEnabled('rag')) {
                summariesHTML += `
                    <p><strong>A:</strong> ${summaryData.ragAnswer}</p>`;
            }

            if (isToggleEnabled('semantic-cache')) {
                summariesHTML += `
                    <div>
                    <p><strong>Cached Query: </strong>${summaryData.cachedQuery}</p>
                    <p><strong>Cached Score: </strong>Cached Score: ${summaryData.cachedScore}</p>
                    </div>`;
            }

            if (summaryData.matchedSummaries && summaryData.matchedSummaries.length > 0) {
                summariesHTML += `<h4>Matched Summaries:</h4><ul>`;
                summariesHTML += summaryData.matchedSummaries.map(s => `
                        <li>
                            <button class="collapsible">${s.summary} (Score: ${parseFloat(s.score).toFixed(2)})</button>
                            <div class="content">
                                <p><strong>Related Utterances:</strong></p>
                                <ul>
                                    ${s.utterances.split("\n").map(utterance => `<li>${utterance}</li>`).join("")}
                                </ul>
                            </div>
                        </li>
                        `).join("");
                summariesHTML += `</ul>`;
            }

            document.getElementById("summaries-response").innerHTML = summariesHTML;
        }

        // Render image-based response if enabled
        if (imageData) {
            let imagesHTML = `<p><strong>Embedding time:</strong> ${imageData.embeddingTime}</p>`

            if (imageData.searchTime) {
                imagesHTML += `<p><strong>Search time:</strong> ${imageData.searchTime}</p>`;
            }

            imagesHTML += `<h4>Matched Images:</h4><ul>`;

            if (imageData.matchedPhotographs && imageData.matchedPhotographs.length > 0) {
                imagesHTML += imageData.matchedPhotographs.map(img => `
                        <li>
                            <p><strong>Image Path:</strong> ${img.imagePath} (Score: ${parseFloat(img.score).toFixed(2)})</p>
                            <img src="${img.imagePath}" alt="Matched Image" style="width:100%;max-width:300px;border-radius:8px;">
                            <p><strong>Description:</strong> ${img.description}</p>
                        </li>
                    `).join("");
            } else {
                imagesHTML += `<p><em>No matching images found.</em></p>`;
            }

            imagesHTML += `</ul>`;

            document.getElementById("images-response").innerHTML = imagesHTML;
        }

        // Render image-text-based response if enabled
        if (imageTextData) {
            let imagesTextHTML = `<p><strong>Embedding time:</strong> ${imageTextData.embeddingTime}</p>`

            if (imageTextData.searchTime) {
                imagesTextHTML += `<p><strong>Search time:</strong> ${imageTextData.searchTime}</p>`;
            }

            imagesTextHTML += `<p><strong>Q:</strong> ${imageTextData.query}</p><h4>Matched Images:</h4><ul>`;

            if (imageTextData.matchedPhotographs && imageTextData.matchedPhotographs.length > 0) {
                imagesTextHTML += imageTextData.matchedPhotographs.map(img => `
                        <li>
                            <p><strong>Image Path:</strong> ${img.imagePath} (Score: ${parseFloat(img.score).toFixed(2)})</p>
                            <img src="${img.imagePath}" alt="Matched Image" style="width:100%;max-width:300px;border-radius:8px;">
                            <p><strong>Description:</strong> ${img.description}</p>
                        </li>
                    `).join("");
            } else {
                imagesTextHTML += `<p><em>No matching images found.</em></p>`;
            }
            imagesTextHTML += `</ul>`;
            document.getElementById("images-text-response").innerHTML = imagesTextHTML;
        }

        attachCollapsibleListeners();
    })
        .catch(error => console.error("Error:", error));
}

// ✅ Function to attach event listeners to collapsible buttons
function attachCollapsibleListeners() {
    document.querySelectorAll(".collapsible").forEach(button => {
        button.addEventListener("click", function() {
            this.nextElementSibling.classList.toggle("active");
        });
    });
}

// ✅ Call this function initially in case collapsibles exist on page load
attachCollapsibleListeners();
