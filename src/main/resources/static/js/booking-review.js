function selectMethod(method) {
    document.getElementById('methodSelection').style.display = 'none';
    const cardFields = document.getElementById('cardPaymentFields');
    const swishFields = document.getElementById('swishPaymentFields');
    const modalTitle = document.getElementById('modalTitle');

    if (method === 'CARD') {
        cardFields.style.display = 'block';
        swishFields.style.display = 'none';
        modalTitle.innerText = 'Betala med Kort';
    } else {
        cardFields.style.display = 'none';
        swishFields.style.display = 'block';
        modalTitle.innerText = 'Betala med Swish';
    }
}

function showMethods() {
    document.getElementById('methodSelection').style.display = 'block';
    document.getElementById('cardPaymentFields').style.display = 'none';
    document.getElementById('swishPaymentFields').style.display = 'none';
    document.getElementById('modalTitle').innerText = 'Betala';
}

function closePaymentModal() {
    const modal = document.getElementById('paymentModal');
    if (modal) {
        modal.style.display = 'none';
        showMethods();
    }
}

function finalizePayment(method) {
    const form = document.getElementById('bookingConfirmationForm');
    
    document.getElementById('hiddenMethod').value = method;
    
    if (method === 'CARD') {
        const cardName = document.getElementById('cardNameInput').value;
        const cardNumber = document.getElementById('cardNumberInput').value;
        const expiry = document.getElementById('expiryInput').value;
        const cvc = document.getElementById('cvcInput').value;
        
        if (!cardName || !cardNumber || !expiry || !cvc) {
            alert('Vänligen fyll i alla kortuppgifter.');
            return;
        }
        
        document.getElementById('hiddenCardHolderName').value = cardName;
        document.getElementById('hiddenCardNumber').value = cardNumber;
        document.getElementById('hiddenExpiryDate').value = expiry;
        document.getElementById('hiddenCvc').value = cvc;
    } else {
        const phone = document.getElementById('phoneInput').value;
        if (!phone) {
            alert('Vänligen fyll i ditt mobilnummer.');
            return;
        }
        document.getElementById('hiddenPhoneNumber').value = phone;
    }
    
    form.submit();
}

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('bookingConfirmationForm');
    const modal = document.getElementById('paymentModal');
    const mainSubmitBtn = document.getElementById('mainSubmitBtn');

    if (form && modal && mainSubmitBtn) {
        form.addEventListener('submit', function(e) {
            // Only intersept if modal is present (immediate payment required)
            // and if modal is not already visible (first click)
            if (modal.style.display !== 'block') {
                e.preventDefault();
                
                // Validate prerequisites
                const discoverySource = document.getElementById('discoverySource').value;
                const termsAccepted = document.querySelector('input[name="termsAccepted"]').checked;
                
                if (!discoverySource) {
                    alert('Vänligen välj hur du hittade resan.');
                    return;
                }
                
                if (!termsAccepted) {
                    alert('Du måste godkänna resevillkoren.');
                    return;
                }
                
                modal.style.display = 'block';
            }
        });
    }

    // Close modal when clicking outside
    window.onclick = function(event) {
        if (event.target == modal) {
            closePaymentModal();
        }
    }
});
