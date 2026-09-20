document.addEventListener('DOMContentLoaded', () => {

    /* Pattern submission confirmation */
    const submissionModal = document.getElementById('submissionModal');

    if (submissionModal) {
        document.body.classList.add('submission-modal-open');

        const closeSubmissionModal = () => {
            submissionModal.remove();
            document.body.classList.remove('submission-modal-open');
        };

        submissionModal.querySelectorAll('[data-close-submission-modal]')
            .forEach((button) => {
                button.addEventListener('click', closeSubmissionModal);
            });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                closeSubmissionModal();
            }
        }, { once: true });
    }


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

    /* AJAX pattern refresh */

    const refreshPatterns =
        document.getElementById('refreshPatterns');

    if (refreshPatterns) {

        refreshPatterns.addEventListener('click', async () => {

            const originalText = refreshPatterns.textContent;

            refreshPatterns.disabled = true;
            refreshPatterns.textContent = 'Refreshing...';

            try {

                const response = await fetch('/api/patterns', {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });

                if (!response.ok) {
                    throw new Error('Pattern request failed');
                }

                const patterns = await response.json();

                const patternMap = new Map(
                    patterns.map(pattern => [String(pattern.id), pattern])
                );

                document
                    .querySelectorAll('.pattern-card[data-pattern-id]')
                    .forEach(card => {

                        const pattern =
                            patternMap.get(card.dataset.patternId);

                        if (!pattern) {
                            return;
                        }

                        card.dataset.downloads = pattern.downloads;
                        card.dataset.price = pattern.price;

                        const titleLink =
                            card.querySelector('.pattern-title-link');

                        if (titleLink) {
                            titleLink.textContent = pattern.title;
                        }

                        const image =
                            card.querySelector('.pattern-card-image');

                        if (image && pattern.imagePath) {
                            image.src = pattern.imagePath;
                            image.alt = pattern.title;
                        }

                        const category =
                            card.querySelector('.pattern-category-value');

                        if (category) {
                            category.textContent = pattern.category;
                        }

                        const downloads =
                            card.querySelector('.pattern-download-count');

                        if (downloads) {
                            downloads.textContent = pattern.downloads;
                        }
                    });

                const toast = document.getElementById('toast');
                const toastMessage = document.getElementById('toastMsg');

                if (toast && toastMessage) {
                    toastMessage.textContent =
                        'Marketplace data refreshed';
                    toast.classList.add('show');

                    clearTimeout(window.patternRefreshToastTimer);

                    window.patternRefreshToastTimer =
                        setTimeout(() => {
                            toast.classList.remove('show');
                        }, 1800);
                }

            } catch (error) {

                const toast = document.getElementById('toast');
                const toastMessage = document.getElementById('toastMsg');

                if (toast && toastMessage) {
                    toastMessage.textContent =
                        'Could not refresh marketplace';
                    toast.classList.add('show');

                    clearTimeout(window.patternRefreshToastTimer);

                    window.patternRefreshToastTimer =
                        setTimeout(() => {
                            toast.classList.remove('show');
                        }, 1800);
                }

            } finally {
                refreshPatterns.disabled = false;
                refreshPatterns.textContent = originalText;
            }
        });
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


    /* Leaderboard tabs */

    const leaderboardTabs =
        document.querySelectorAll('.lb-tab');

    const leaderboardList =
        document.getElementById('leaderboardList');

    const leaderboardEmpty =
        document.getElementById('leaderboardEmpty');

    const leaderboardStatus =
        document.getElementById('leaderboardStatus');

    function rankIcon(rank) {

        if (rank > 3) {
            const number = document.createElement('span');
            number.textContent = rank;
            return number;
        }

        const svg = document.createElementNS(
            'http://www.w3.org/2000/svg',
            'svg'
        );

        svg.setAttribute('width', '22');
        svg.setAttribute('height', '22');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('fill', 'none');
        svg.setAttribute('stroke', 'currentColor');
        svg.setAttribute('stroke-width', '1.8');
        svg.setAttribute('stroke-linecap', 'round');
        svg.setAttribute('stroke-linejoin', 'round');
        svg.setAttribute('aria-hidden', 'true');

        [
            ['path', {d: 'M8 21h8'}],
            ['path', {d: 'M12 17v4'}],
            ['path', {d: 'M7 4h10v3a5 5 0 0 1-10 0V4z'}],
            ['path', {d: 'M7 6H4a3 3 0 0 0 3 3'}],
            ['path', {d: 'M17 6h3a3 3 0 0 1-3 3'}],
            ['path', {d: 'M9 2h6'}]
        ].forEach(([tag, attributes]) => {
            const path = document.createElementNS(
                'http://www.w3.org/2000/svg',
                tag
            );

            Object.entries(attributes).forEach(([key, value]) => {
                path.setAttribute(key, value);
            });

            svg.appendChild(path);
        });

        return svg;
    }

    function createLeaderboardItem(entry) {

        const item = document.createElement('div');
        item.className = 'lb-item';

        const rank = document.createElement('div');
        rank.className = 'lb-rank';
        rank.appendChild(rankIcon(entry.rank));

        const avatar = document.createElement('div');
        avatar.className = 'lb-avatar';

        if (entry.profileImagePath) {
            const image = document.createElement('img');
            image.src = entry.profileImagePath;
            image.alt = 'Profile';

            if (entry.profileImageObjectPosition) {
                image.style.objectPosition =
                    entry.profileImageObjectPosition;
            }

            avatar.appendChild(image);
        } else {
            const initial = document.createElement('span');
            initial.className = 'avatar-initial';
            initial.textContent =
                (entry.username || 'U').charAt(0).toUpperCase();
            avatar.appendChild(initial);
        }

        const info = document.createElement('div');
        info.className = 'lb-info';

        const name = document.createElement('div');
        name.className = 'name';
        name.textContent = entry.name || entry.username || 'User';

        const badge = document.createElement('div');
        badge.className = 'badge';
        badge.textContent = metricLabel(entry.metric);

        info.appendChild(name);
        info.appendChild(badge);

        const score = document.createElement('div');
        score.className = 'lb-score';
        score.textContent = formatScore(entry);

        item.appendChild(rank);
        item.appendChild(avatar);
        item.appendChild(info);
        item.appendChild(score);

        return item;
    }

    function metricLabel(metric) {
        if (metric === 'sales') {
            return 'Top Sellers';
        }

        if (metric === 'downloads') {
            return 'Most Downloaded';
        }

        return 'Top Creators';
    }

    function formatScore(entry) {
        if (entry.metric === 'sales') {
            return `${entry.score} sale${entry.score === 1 ? '' : 's'}`;
        }

        if (entry.metric === 'downloads') {
            return `${entry.score} download${entry.score === 1 ? '' : 's'}`;
        }

        return `${entry.score} XP`;
    }

    async function loadLeaderboard(type, activeTab) {

        leaderboardTabs.forEach(tab => {
            tab.classList.toggle('active', tab === activeTab);
            tab.disabled = true;
        });

        if (leaderboardStatus) {
            leaderboardStatus.textContent = 'Updating leaderboard...';
        }

        try {
            const response = await fetch(
                `/api/leaderboard?type=${encodeURIComponent(type)}`,
                {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                }
            );

            if (!response.ok) {
                throw new Error('Leaderboard request failed');
            }

            const entries = await response.json();

            if (leaderboardList) {
                leaderboardList.replaceChildren();
            }

            if (leaderboardEmpty) {
                leaderboardEmpty.hidden = entries.length > 0;
            }

            if (entries.length > 0 && leaderboardList) {
                entries.forEach(entry => {
                    leaderboardList.appendChild(
                        createLeaderboardItem(entry)
                    );
                });

                leaderboardList.hidden = false;
            } else if (leaderboardList) {
                leaderboardList.hidden = true;
            }

            if (leaderboardStatus) {
                leaderboardStatus.textContent = '';
            }

        } catch (error) {

            if (leaderboardStatus) {
                leaderboardStatus.textContent =
                    'Could not refresh leaderboard. Please try again.';
            }

        } finally {
            leaderboardTabs.forEach(tab => {
                tab.disabled = false;
            });
        }
    }

    if (leaderboardTabs.length && leaderboardList) {

        leaderboardTabs.forEach(tab => {
            tab.addEventListener('click', () => {
                loadLeaderboard(
                    tab.dataset.leaderboardType,
                    tab
                );
            });
        });
    }


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