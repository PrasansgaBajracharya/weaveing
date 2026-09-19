document.addEventListener("DOMContentLoaded", () => {

    const priceRadios = document.querySelectorAll('input[name="priceType"]');
    const priceInput = document.getElementById("price");

    function updatePriceState() {
        const selected = document.querySelector('input[name="priceType"]:checked');

        if (!selected || !priceInput) {
            return;
        }

        if (selected.value === "free") {
            priceInput.value = "0";
            priceInput.disabled = true;
        } else {
            priceInput.disabled = false;
        }
    }

    priceRadios.forEach((radio) => {
        radio.addEventListener("change", updatePriceState);
    });

    updatePriceState();

    const imageInput = document.getElementById("image");
    const patternInput = document.getElementById("patternFile");

    function updateFileName(input) {
        if (!input || !input.files || !input.files.length) {
            return;
        }

        const uploadBox = input.previousElementSibling;

        if (!uploadBox) {
            return;
        }

        const strong = uploadBox.querySelector("strong");

        if (strong) {
            strong.textContent = input.files[0].name;
        }
    }

    imageInput?.addEventListener("change", () => updateFileName(imageInput));
    patternInput?.addEventListener("change", () => updateFileName(patternInput));

    const imagePreviewInput = document.getElementById("image");
    const imagePreviewEditor = document.getElementById("imagePreviewEditor");
    const imagePreviewFrame = imagePreviewEditor?.querySelector(".image-preview-frame");
    const imagePreview = document.getElementById("imagePreview");
    const imagePreviewFallback = document.getElementById("imagePreviewFallback");
    const imagePositionX = document.getElementById("imagePositionX");
    const imagePositionY = document.getElementById("imagePositionY");

    if (!imagePreviewEditor || !imagePreviewFrame || !imagePositionX || !imagePositionY) {
        return;
    }

    imagePreviewFrame.setAttribute(
        "title",
        "Click the part of the image you want to keep visible"
    );

    const previewHeading = imagePreviewEditor.querySelector(".image-preview-copy strong");
    const previewHelp = imagePreviewEditor.querySelector(".image-preview-copy p");

    if (previewHeading) {
        previewHeading.textContent = "Set your cover focus";
    }

    if (previewHelp) {
        previewHelp.textContent =
            "Click the part of the image you want to keep visible. The crosshair marks your focus, and the square shows the marketplace crop.";
    }

    let focusMarker = imagePreviewFrame.querySelector(".image-focus-marker");

    if (!focusMarker) {
        focusMarker = document.createElement("span");
        focusMarker.className = "image-focus-marker";
        focusMarker.setAttribute("aria-hidden", "true");
        imagePreviewFrame.appendChild(focusMarker);
    }

    function clamp(value) {
        return Math.max(0, Math.min(100, Math.round(value)));
    }

    function placeMarker(x, y) {
        focusMarker.style.left = `${x}%`;
        focusMarker.style.top = `${y}%`;
    }

    function setPosition(x, y) {
        const safeX = clamp(x);
        const safeY = clamp(y);

        imagePositionX.value = safeX;
        imagePositionY.value = safeY;

        const image = imagePreviewFallback?.hidden === false
            ? imagePreviewFallback
            : imagePreview;

        if (image) {
            image.style.objectPosition = `${safeX}% ${safeY}%`;
        }

        placeMarker(safeX, safeY);
    }

    imagePreviewFrame.addEventListener("click", (event) => {
        const rect = imagePreviewFrame.getBoundingClientRect();

        if (!rect.width || !rect.height) {
            return;
        }

        setPosition(
            ((event.clientX - rect.left) / rect.width) * 100,
            ((event.clientY - rect.top) / rect.height) * 100
        );
    });

    imagePreviewInput?.addEventListener("change", () => {
        const file = imagePreviewInput.files?.[0];

        if (!file) {
            return;
        }

        const url = URL.createObjectURL(file);
        const target = imagePreviewFallback || imagePreview;

        if (!target) {
            return;
        }

        target.hidden = false;
        target.src = url;
        target.style.objectPosition =
            `${imagePositionX.value || 50}% ${imagePositionY.value || 50}%`;

        if (target !== imagePreview && imagePreview) {
            imagePreview.hidden = true;
        }

        imagePreviewEditor.hidden = false;
        imagePreviewEditor.style.display = "grid";

        placeMarker(
            Number(imagePositionX.value || 50),
            Number(imagePositionY.value || 50)
        );
    });

    placeMarker(
        Number(imagePositionX.value || 50),
        Number(imagePositionY.value || 50)
    );
});
