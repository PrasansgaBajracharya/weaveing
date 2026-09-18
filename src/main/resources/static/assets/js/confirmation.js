
(function () {
    let pendingAction = null;

    function createModal() {
        const overlay = document.createElement('div');
        overlay.className = 'confirmation-overlay';
        overlay.innerHTML = `
            <div class="confirmation-modal" role="dialog" aria-modal="true" aria-labelledby="confirmationTitle">
                <div class="confirmation-mark" aria-hidden="true">✦</div>
                <h2 id="confirmationTitle">Are you sure?</h2>
                <p id="confirmationMessage">Please confirm this action.</p>
                <div class="confirmation-actions">
                    <button type="button" class="confirmation-cancel">Cancel</button>
                    <button type="button" class="confirmation-accept">Confirm</button>
                </div>
            </div>`;

        document.body.appendChild(overlay);

        const close = () => {
            overlay.classList.remove('is-visible');
            setTimeout(() => overlay.remove(), 180);
            pendingAction = null;
        };

        overlay.querySelector('.confirmation-cancel').addEventListener('click', close);
        overlay.addEventListener('click', event => {
            if (event.target === overlay) close();
        });

        overlay.querySelector('.confirmation-accept').addEventListener('click', () => {
            if (pendingAction) pendingAction();
            close();
        });

        return overlay;
    }

    document.addEventListener('click', event => {
        const trigger = event.target.closest('[data-confirm]');
        if (!trigger) return;

        if (trigger.tagName === 'FORM') return;

        event.preventDefault();
        pendingAction = () => {
            if (trigger.tagName === 'A') {
                window.location.href = trigger.href;
            } else if (trigger.form) {
                trigger.form.submit();
            }
        };

        const overlay = createModal();
        overlay.querySelector('#confirmationTitle').textContent =
            trigger.dataset.confirmTitle || 'Are you sure?';
        overlay.querySelector('#confirmationMessage').textContent =
            trigger.dataset.confirm || 'Please confirm this action.';
        overlay.querySelector('.confirmation-accept').textContent =
            trigger.dataset.confirmAccept || 'Confirm';
        requestAnimationFrame(() => overlay.classList.add('is-visible'));
    });

    document.addEventListener('submit', event => {
        const form = event.target.closest('form[data-confirm]');
        if (!form || form.dataset.confirmOpen === 'true') return;

        event.preventDefault();
        form.dataset.confirmOpen = 'true';
        pendingAction = () => {
            form.dataset.confirmOpen = 'true';
            form.submit();
        };

        const overlay = createModal();
        overlay.querySelector('#confirmationTitle').textContent =
            form.dataset.confirmTitle || 'Are you sure?';
        overlay.querySelector('#confirmationMessage').textContent =
            form.dataset.confirm || 'Please confirm this action.';
        overlay.querySelector('.confirmation-accept').textContent =
            form.dataset.confirmAccept || 'Confirm';
        requestAnimationFrame(() => overlay.classList.add('is-visible'));
    });
})();
