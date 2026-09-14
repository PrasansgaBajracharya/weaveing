document.addEventListener("DOMContentLoaded", () => {

    const priceRadios = document.querySelectorAll(
        'input[name="priceType"]'
    );

    const priceInput = document.getElementById("price");


    function updatePriceState() {

        const selected = document.querySelector(
            'input[name="priceType"]:checked'
        );

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

        radio.addEventListener(
            "change",
            updatePriceState
        );

    });


    updatePriceState();



    /* ========================================================
       FILE NAME DISPLAY
       ======================================================== */

    const imageInput = document.getElementById("image");
    const patternInput = document.getElementById("patternFile");


    function updateFileName(input) {

        if (!input || !input.files || !input.files.length) {
            return;
        }


        const uploadBox =
            input.previousElementSibling;


        if (!uploadBox) {
            return;
        }


        const strong =
            uploadBox.querySelector("strong");


        if (!strong) {
            return;
        }


        strong.textContent =
            input.files[0].name;

    }


    if (imageInput) {

        imageInput.addEventListener(
            "change",
            () => updateFileName(imageInput)
        );

    }


    if (patternInput) {

        patternInput.addEventListener(
            "change",
            () => updateFileName(patternInput)
        );

    }

});