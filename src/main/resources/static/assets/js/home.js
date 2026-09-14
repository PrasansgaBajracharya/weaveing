document.addEventListener('DOMContentLoaded', () => {

    /* =========================================================
       MOBILE MENU
    ========================================================= */

    const menuToggle = document.querySelector('.menu-toggle');
    const mobileMenu = document.querySelector('.mobile-menu');

    if (menuToggle && mobileMenu) {
        menuToggle.addEventListener('click', () => {
            mobileMenu.classList.toggle('open');
            menuToggle.classList.toggle('open');
        });

        mobileMenu.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                mobileMenu.classList.remove('open');
                menuToggle.classList.remove('open');
            });
        });
    }


    /* =========================================================
       FILTER / SORT
    ========================================================= */

    const filterToggle = document.getElementById('filterToggle');
    const filterPanel = document.getElementById('filterPanel');
    const clearFilters = document.getElementById('clearFilters');

    const categoryFilter =
        document.getElementById('categoryFilter');

    const difficultyFilter =
        document.getElementById('difficultyFilter');

    const priceFilter =
        document.getElementById('priceFilter');

    const sortFilter =
        document.getElementById('sortFilter');

    const searchInput =
        document.querySelector('.search-wrap input[name="search"]');


    /* ---------- Open / Close Filter Panel ---------- */

    if (filterToggle && filterPanel) {

        filterToggle.addEventListener(
            'click',
            () => {

                const open =
                    filterPanel.classList.toggle('open');

                filterToggle.classList.toggle(
                    'open',
                    open
                );

                filterToggle.setAttribute(
                    'aria-expanded',
                    String(open)
                );

                filterPanel.setAttribute(
                    'aria-hidden',
                    String(!open)
                );
            }
        );
    }


    /* ---------- Build Server-Side Filter URL ---------- */

    function buildFilterUrl() {

        const params =
            new URLSearchParams();

        const search =
            searchInput
                ? searchInput.value.trim()
                : '';

        const category =
            categoryFilter
                ? categoryFilter.value
                : 'all';

        const difficulty =
            difficultyFilter
                ? difficultyFilter.value
                : 'all';

        const priceType =
            priceFilter
                ? priceFilter.value
                : 'all';

        const sort =
            sortFilter
                ? sortFilter.value
                : 'newest';


        if (search) {
            params.set('search', search);
        }

        if (category !== 'all') {
            params.set('category', category);
        }

        if (difficulty !== 'all') {
            params.set('difficulty', difficulty);
        }

        if (priceType !== 'all') {
            params.set('priceType', priceType);
        }

        if (sort !== 'newest') {
            params.set('sort', sort);
        }

        const query =
            params.toString();

        return query
            ? `/home?${query}`
            : '/home';
    }


    /* ---------- Apply Filters Through Backend ---------- */

    [
        categoryFilter,
        difficultyFilter,
        priceFilter,
        sortFilter
    ]
        .filter(Boolean)
        .forEach(control => {

            control.addEventListener(
                'change',
                () => {
                    window.location.href =
                        buildFilterUrl();
                }
            );
        });


    /* ---------- Clear Filters ---------- */

    if (clearFilters) {

        clearFilters.addEventListener(
            'click',
            () => {
                window.location.href = '/home';
            }
        );
    }


    /* ---------- Search Form ---------- */

    const searchForm =
        document.querySelector('.search-wrap');

    if (searchForm) {

        searchForm.addEventListener(
            'submit',
            () => {

                if (categoryFilter) {
                    searchForm.querySelector(
                        'input[name="category"]'
                    ).value =
                        categoryFilter.value;
                }

                if (difficultyFilter) {
                    searchForm.querySelector(
                        'input[name="difficulty"]'
                    ).value =
                        difficultyFilter.value;
                }

                if (priceFilter) {
                    searchForm.querySelector(
                        'input[name="priceType"]'
                    ).value =
                        priceFilter.value;
                }

                if (sortFilter) {
                    searchForm.querySelector(
                        'input[name="sort"]'
                    ).value =
                        sortFilter.value;
                }
            }
        );
    }

    /* Wishlist hearts */

    const wishlistButtons =
        document.querySelectorAll('.fav-btn');

    wishlistButtons.forEach(button => {

        button.addEventListener('click', async event => {

            event.preventDefault();
            event.stopPropagation();

            const patternId = button.dataset.patternId;

            if (!patternId || button.disabled) {
                return;
            }

            const wasSaved =
                button.classList.contains('is-saved');

            button.disabled = true;

            try {

                const response = await fetch(
                    `/wishlist/toggle/${patternId}`,
                    {
                        method: 'POST',
                        headers: {
                            'X-Requested-With': 'XMLHttpRequest'
                        }
                    }
                );

                if (!response.ok) {
                    throw new Error('Wishlist request failed');
                }

                const result = await response.json();

                if (!result.success) {
                    throw new Error('Wishlist request failed');
                }

                button.classList.toggle(
                    'is-saved',
                    result.saved
                );

                button.setAttribute(
                    'aria-pressed',
                    String(result.saved)
                );

                button.setAttribute(
                    'aria-label',
                    result.saved
                        ? 'Remove from wishlist'
                        : 'Save to wishlist'
                );

                const saveCount =
                    document.querySelector(
                        `.wishlist-save-count[data-pattern-id="${patternId}"]`
                    );

                if (saveCount) {
                    saveCount.textContent =
                        result.count;
                }

                const toast =
                    document.getElementById('toast');

                const toastMessage =
                    document.getElementById('toastMsg');

                if (toast && toastMessage) {
                    toastMessage.textContent = result.saved
                        ? 'Saved to wishlist'
                        : 'Removed from wishlist';

                    toast.classList.add('show');

                    clearTimeout(window.wishlistToastTimer);

                    window.wishlistToastTimer =
                        setTimeout(() => {
                            toast.classList.remove('show');
                        }, 1800);
                }

            } catch (error) {

                button.classList.toggle(
                    'is-saved',
                    wasSaved
                );

                const toast =
                    document.getElementById('toast');

                const toastMessage =
                    document.getElementById('toastMsg');

                if (toast && toastMessage) {
                    toastMessage.textContent =
                        'Could not update wishlist';
                    toast.classList.add('show');

                    clearTimeout(window.wishlistToastTimer);

                    window.wishlistToastTimer =
                        setTimeout(() => {
                            toast.classList.remove('show');
                        }, 1800);
                }

            } finally {
                button.disabled = false;
            }
        });
    });

    /* =========================================================
       COMMUNITY LIKE BUTTONS
    ========================================================= */

    const likeButtons =
        document.querySelectorAll('.community-like');

    likeButtons.forEach(button => {

        button.addEventListener('click', event => {

            event.preventDefault();

            button.classList.toggle('liked');

            const count =
                button.querySelector('.like-count');

            if (!count) {
                return;
            }

            let current =
                parseInt(
                    count.textContent,
                    10
                ) || 0;


            if (button.classList.contains('liked')) {

                current += 1;

            } else {

                current = Math.max(
                    0,
                    current - 1
                );
            }


            count.textContent = current;
        });
    });


    /* =========================================================
       SCROLL REVEAL
    ========================================================= */

    const revealElements =
        document.querySelectorAll(
            '.reveal, .reveal-scale'
        );


    if ('IntersectionObserver' in window) {

        const revealObserver =
            new IntersectionObserver(
                entries => {

                    entries.forEach(entry => {

                        if (entry.isIntersecting) {

                            /*
                             * IMPORTANT:
                             * landing.css uses .in-view
                             * to reveal these elements.
                             */

                            entry.target.classList.add(
                                'in-view'
                            );

                            revealObserver.unobserve(
                                entry.target
                            );
                        }
                    });

                },
                {
                    threshold: 0.1
                }
            );


        revealElements.forEach(element => {

            revealObserver.observe(element);

        });

    } else {

        revealElements.forEach(element => {

            element.classList.add('in-view');

        });
    }


    /* =========================================================
       BACK TO TOP
    ========================================================= */

    const backToTop =
        document.querySelector('.back-to-top');


    if (backToTop) {

        window.addEventListener(
            'scroll',
            () => {

                if (window.scrollY > 500) {

                    backToTop.classList.add(
                        'show'
                    );

                } else {

                    backToTop.classList.remove(
                        'show'
                    );
                }
            }
        );


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


    /* =========================================================
       RIPPLE EFFECT
    ========================================================= */

    const rippleButtons =
        document.querySelectorAll(
            '.btn, .filter-toggle'
        );


    rippleButtons.forEach(button => {

        button.addEventListener(
            'click',
            function(event) {

                const ripple =
                    document.createElement('span');

                ripple.classList.add('ripple');


                const rect =
                    this.getBoundingClientRect();


                const size =
                    Math.max(
                        rect.width,
                        rect.height
                    );


                ripple.style.width =
                    `${size}px`;

                ripple.style.height =
                    `${size}px`;

                ripple.style.left =
                    `${event.clientX - rect.left - size / 2}px`;

                ripple.style.top =
                    `${event.clientY - rect.top - size / 2}px`;


                this.appendChild(ripple);


                setTimeout(() => {

                    ripple.remove();

                }, 600);
            }
        );
    });


    /* =========================================================
       CLOSE FILTER WHEN CLICKING OUTSIDE
    ========================================================= */

    document.addEventListener(
        'click',
        event => {

            if (
                !filterPanel
                || !filterToggle
                || !filterPanel.classList.contains('open')
            ) {
                return;
            }


            const clickedInsideFilter =
                filterPanel.contains(event.target)
                || filterToggle.contains(event.target);


            if (!clickedInsideFilter) {

                filterPanel.classList.remove(
                    'open'
                );

                filterToggle.classList.remove(
                    'open'
                );

                filterToggle.setAttribute(
                    'aria-expanded',
                    'false'
                );

                filterPanel.setAttribute(
                    'aria-hidden',
                    'true'
                );
            }
        }
    );

});