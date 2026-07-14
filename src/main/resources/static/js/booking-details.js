function preparePayment(amount) {
    if (!amount || isNaN(amount) || amount <= 0) {
        alert('Vänligen ange ett giltigt belopp.');
        return;
    }

    const hiddenAmount = document.getElementById('hiddenAmount');
    const displayAmount = document.getElementById('displayAmount');
    
    hiddenAmount.value = amount;
    displayAmount.innerText = amount.toLocaleString('sv-SE').replace(',', ' ');

    showMethods();
    document.getElementById('paymentModal').style.display = 'block';
}

function prepareCustomPayment() {
    const customInput = document.getElementById('customAmountInput');
    const amount = parseInt(customInput.value);
    
    if (isNaN(amount) || amount <= 0) {
        alert('Vänligen ange ett giltigt belopp.');
        customInput.focus();
        return;
    }
    
    const max = parseInt(customInput.getAttribute('max'));
    if (amount > max) {
        alert('Beloppet kan inte vara högre än kvarstående belopp (' + max + ' kr).');
        customInput.focus();
        return;
    }
    
    preparePayment(amount);
}

function selectMethod(method) {
    const modalTitle = document.getElementById('modalTitle');
    const methodSelection = document.getElementById('methodSelection');
    const cardFields = document.getElementById('cardFields');
    const swishFields = document.getElementById('swishFields');
    const hiddenMethod = document.getElementById('hiddenMethod');
    const submitBtn = document.getElementById('submitPaymentBtn');

    hiddenMethod.value = method;
    methodSelection.style.display = 'none';
    submitBtn.style.display = 'block';

    if (method === 'CARD') {
        modalTitle.innerText = 'Betala med Kort';
        cardFields.style.display = 'block';
        swishFields.style.display = 'none';
    } else if (method === 'SWISH') {
        modalTitle.innerText = 'Betala med Swish';
        cardFields.style.display = 'none';
        swishFields.style.display = 'block';
    }
}

function showMethods() {
    document.getElementById('modalTitle').innerText = 'Betala';
    document.getElementById('methodSelection').style.display = 'block';
    document.getElementById('cardFields').style.display = 'none';
    document.getElementById('swishFields').style.display = 'none';
    document.getElementById('submitPaymentBtn').style.display = 'none';
    document.getElementById('hiddenMethod').value = '';
}

function closePaymentModal() {
    document.getElementById('paymentModal').style.display = 'none';
}

window.onclick = function(event) {
    const modal = document.getElementById('paymentModal');
    if (event.target == modal) {
        modal.style.display = 'none';
    }
}
