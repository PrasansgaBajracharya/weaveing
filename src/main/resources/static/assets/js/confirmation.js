
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

    function setupPatternManagement() {
        const patternsSection = document.getElementById('patterns');
        const grid = document.getElementById('myPatternGrid');

        if (!patternsSection || !grid || document.getElementById('patternEditToggle')) {
            return;
        }

        const heading = patternsSection.querySelector('.section-heading');
        const postButton = heading?.querySelector('a[href*="/patterns/new"]');

        if (!heading || !postButton) {
            return;
        }

        const actions = document.createElement('div');
        actions.className = 'my-pattern-actions';

        const toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.id = 'patternEditToggle';
        toggle.className = 'pattern-edit-toggle';
        toggle.setAttribute('aria-expanded', 'false');
        toggle.setAttribute('aria-controls', 'myPatternGrid');
        toggle.textContent = 'Edit patterns';

        postButton.replaceWith(actions);
        actions.append(toggle, postButton);

        grid.id = 'myPatternGrid';

        toggle.addEventListener('click', () => {
            const editing = grid.classList.toggle('is-managing');
            toggle.setAttribute('aria-expanded', String(editing));
            toggle.textContent = editing ? 'Done' : 'Edit patterns';
            toggle.classList.toggle('is-active', editing);
        });

        grid.querySelectorAll('form').forEach(form => {
            const removeButton = form.querySelector('.manage-button.remove');

            if (!removeButton || form.dataset.confirm) {
                return;
            }

            form.dataset.confirm =
                'Remove this pattern from the marketplace? Existing purchases will remain available.';
            form.dataset.confirmTitle = 'Remove pattern?';
            form.dataset.confirmAccept = 'Remove pattern';
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupPatternManagement);
    } else {
        setupPatternManagement();
    }

})();
