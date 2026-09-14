/* weave.ing — Landing Page JavaScript */
/* NAVBAR SCROLL + BACK TO TOP*/
const navbar =
    document.getElementById('navbar');

const backToTop =
    document.getElementById('backToTop');

window.addEventListener(
    'scroll',
    () => {

        const scrollY =
            window.scrollY;

        if (navbar) {

            navbar.classList.toggle(
                'scrolled',
                scrollY > 40
            );

        }

        if (backToTop) {

            backToTop.classList.toggle(
                'show',
                scrollY > 500
            );

        }

    },
    { passive: true }
);


/* ============================================================
   MOBILE MENU
============================================================ */

const hamburger =
    document.getElementById('hamburger');

const mobileMenu =
    document.getElementById('mobileMenu');


if (hamburger && mobileMenu) {

    hamburger.addEventListener(
        'click',
        () => {

            const isOpen =
                mobileMenu.classList.toggle('open');

            hamburger.classList.toggle(
                'open',
                isOpen
            );

            hamburger.setAttribute(
                'aria-expanded',
                String(isOpen)
            );

            document.body.classList.toggle(
                'menu-open',
                isOpen
            );

        }
    );


    mobileMenu
        .querySelectorAll('a')
        .forEach(link => {

            link.addEventListener(
                'click',
                () => {

                    mobileMenu.classList.remove(
                        'open'
                    );

                    hamburger.classList.remove(
                        'open'
                    );

                    hamburger.setAttribute(
                        'aria-expanded',
                        'false'
                    );

                    document.body.classList.remove(
                        'menu-open'
                    );

                }
            );

        });

}


/* ============================================================
   SCROLL REVEAL
============================================================ */

const revealElements =
    document.querySelectorAll(
        '.reveal, .reveal-scale'
    );


if ('IntersectionObserver' in window) {

    const revealObserver =
        new IntersectionObserver(
            entries => {

                entries.forEach(entry => {

                    if (!entry.isIntersecting) {
                        return;
                    }

                    entry.target.classList.add(
                        'in-view'
                    );

                    revealObserver.unobserve(
                        entry.target
                    );

                });

            },
            {
                threshold: 0.12
            }
        );


    revealElements.forEach(
        element => {

            revealObserver.observe(
                element
            );

        }
    );

} else {

    revealElements.forEach(
        element => {

            element.classList.add(
                'in-view'
            );

        }
    );

}


/* ============================================================
   ANIMATED STAT COUNTERS
============================================================ */

const stats =
    document.querySelectorAll(
        '.stat-num'
    );


if ('IntersectionObserver' in window) {

    const statObserver =
        new IntersectionObserver(
            entries => {

                entries.forEach(entry => {

                    if (!entry.isIntersecting) {
                        return;
                    }

                    const element =
                        entry.target;

                    const target =
                        parseInt(
                            element.dataset.count,
                            10
                        );


                    if (
                        Number.isNaN(target)
                    ) {
                        return;
                    }


                    let current = 0;

                    const duration = 1200;

                    const startTime =
                        performance.now();


                    const animate =
                        currentTime => {

                            const elapsed =
                                currentTime -
                                startTime;

                            const progress =
                                Math.min(
                                    elapsed /
                                    duration,
                                    1
                                );


                            const eased =
                                1 -
                                Math.pow(
                                    1 - progress,
                                    3
                                );


                            current =
                                Math.floor(
                                    eased * target
                                );


                            element.textContent =
                                current;


                            if (progress < 1) {

                                requestAnimationFrame(
                                    animate
                                );

                            } else {

                                element.textContent =
                                    target + '+';

                            }

                        };


                    requestAnimationFrame(
                        animate
                    );


                    statObserver.unobserve(
                        element
                    );

                });

            },
            {
                threshold: 0.6
            }
        );


    stats.forEach(stat => {

        statObserver.observe(stat);

    });

} else {

    stats.forEach(stat => {

        const target =
            stat.dataset.count;

        stat.textContent =
            target + '+';

    });

}


/* ============================================================
   FAVORITE / WISHLIST
============================================================ */

const favoriteButtons =
    document.querySelectorAll(
        '.fav-btn'
    );


favoriteButtons.forEach(
    button => {

        button.addEventListener(
            'click',
            () => {

                const isActive =
                    button.classList.toggle(
                        'active'
                    );


                const message =
                    isActive
                        ? 'Saved to wishlist'
                        : 'Removed from wishlist';


                showToast(message);

            }
        );

    }
);


/* ============================================================
   TOAST
============================================================ */

const toast =
    document.getElementById('toast');

const toastMessage =
    document.getElementById('toastMsg');

let toastTimer = null;


function showToast(message) {

    if (!toast || !toastMessage) {
        return;
    }


    toastMessage.textContent =
        message;


    toast.classList.add(
        'show'
    );


    if (toastTimer) {

        clearTimeout(
            toastTimer
        );

    }


    toastTimer =
        setTimeout(
            () => {

                toast.classList.remove(
                    'show'
                );

            },
            2200
        );

}


/* ============================================================
   BACK TO TOP
============================================================ */

if (backToTop) {

    backToTop.addEventListener(
        'click',
        () => {

            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });

        }
    );

}


/* ============================================================
   BUTTON RIPPLE
============================================================ */

document
    .querySelectorAll('.btn')
    .forEach(button => {

        button.addEventListener(
            'click',
            function (event) {

                const rect =
                    this.getBoundingClientRect();


                const ripple =
                    document.createElement(
                        'span'
                    );


                ripple.className =
                    'ripple';


                const size =
                    Math.max(
                        rect.width,
                        rect.height
                    );


                ripple.style.width =
                    size + 'px';

                ripple.style.height =
                    size + 'px';


                ripple.style.left =
                    (
                        event.clientX -
                        rect.left -
                        size / 2
                    ) + 'px';


                ripple.style.top =
                    (
                        event.clientY -
                        rect.top -
                        size / 2
                    ) + 'px';


                this.appendChild(
                    ripple
                );


                setTimeout(
                    () => {

                        ripple.remove();

                    },
                    650
                );

            }
        );

    });


/* ============================================================
   HERO CARD MOUSE TILT
============================================================ */

const heroLeft =
    document.getElementById(
        'heroLeft'
    );


if (
    heroLeft &&
    window.matchMedia(
        '(pointer: fine)'
    ).matches
) {

    heroLeft.addEventListener(
        'mousemove',
        event => {

            const rect =
                heroLeft.getBoundingClientRect();


            const x =
                (
                    event.clientX -
                    rect.left
                ) /
                rect.width -
                0.5;


            const y =
                (
                    event.clientY -
                    rect.top
                ) /
                rect.height -
                0.5;


            heroLeft
                .querySelectorAll(
                    '.float-card'
                )
                .forEach(
                    (card, index) => {

                        const depth =
                            (
                                index % 3 +
                                1
                            ) * 5;


                        const moveX =
                            x * depth;


                        const moveY =
                            y * depth;


                        card.style.translate =
                            `${moveX}px ${moveY}px`;

                    }
                );

        }
    );


    heroLeft.addEventListener(
        'mouseleave',
        () => {

            heroLeft
                .querySelectorAll(
                    '.float-card'
                )
                .forEach(
                    card => {

                        card.style.translate =
                            '';

                    }
                );

        }
    );

}


/* ============================================================
   ESC KEY — CLOSE MOBILE MENU
============================================================ */

document.addEventListener(
    'keydown',
    event => {

        if (
            event.key !== 'Escape' ||
            !mobileMenu ||
            !hamburger
        ) {
            return;
        }


        mobileMenu.classList.remove(
            'open'
        );

        hamburger.classList.remove(
            'open'
        );

        hamburger.setAttribute(
            'aria-expanded',
            'false'
        );

        document.body.classList.remove(
            'menu-open'
        );

    }
);