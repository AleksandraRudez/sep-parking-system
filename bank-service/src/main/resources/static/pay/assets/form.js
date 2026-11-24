// Pomoc pri unosu i provera pre slanja. Server uvek ponovo proverava sve podatke.
(function () {
  const form = document.getElementById('payment-form');
  if (!form) return;
  const pan = form.elements.pan;
  const expiry = form.elements.expiry;
  const cvv = form.elements.cvv;

  function luhn(digits) {
    let sum = 0;
    let double = false;
    for (let i = digits.length - 1; i >= 0; i--) {
      let d = digits.charCodeAt(i) - 48;
      if (double) {
        d *= 2;
        if (d > 9) d -= 9;
      }
      sum += d;
      double = !double;
    }
    return digits.length >= 12 && sum % 10 === 0;
  }

  pan.addEventListener('input', () => {
    const digits = pan.value.replace(/\D/g, '').slice(0, 19);
    pan.value = digits.replace(/(.{4})/g, '$1 ').trim();
    pan.setCustomValidity(digits.length >= 12 && !luhn(digits) ? 'Broj kartice nije ispravan' : '');
  });

  expiry.addEventListener('input', () => {
    const digits = expiry.value.replace(/\D/g, '').slice(0, 4);
    expiry.value = digits.length > 2 ? digits.slice(0, 2) + '/' + digits.slice(2) : digits;
  });

  cvv.addEventListener('input', () => {
    cvv.value = cvv.value.replace(/\D/g, '').slice(0, 3);
  });

  form.addEventListener('submit', () => {
    // Sprecava dvostruki klik; server svakako dozvoljava samo jedan pokusaj
    form.querySelector('button[type=submit]').disabled = true;
  });
})();
