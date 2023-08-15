const playlistSelect = document.getElementById("playlistSelect");
const newPlaylistNameInput = document.getElementById("newPlaylistName");
const labelInput = document.getElementById("inputLabel");
const newPlaylistNameDiv = document.getElementById("newPlaylistNameDiv");
const playlistSelectDiv = document.getElementById("playlistSelectDiv");
const importTypeRadio = document.upload.importType;

for (var i = 0; i < importTypeRadio.length; i++) {
    importTypeRadio[i].addEventListener('change', function() {
        if (this.value == 'LIKED_SONGS') {
            playlistSelectDiv.classList.add('hide');
        } else {
            playlistSelectDiv.classList.remove('hide');
        }
    });
}

playlistSelect.addEventListener('change', function() {
    if (playlistSelect.value === 'newPlaylist') {
        newPlaylistNameInput.removeAttribute('disabled');
        newPlaylistNameDiv.classList.remove('hide');
    } else {
        newPlaylistNameInput.setAttribute('disabled', '');
        newPlaylistNameDiv.classList.add('hide');
    }
});

document.upload.addEventListener("submit", function() {
    const loadingEl = document.createElement("div");
    document.body.prepend(loadingEl);
    loadingEl.classList.add("page-loader");
    loadingEl.classList.add("flex-column");
    loadingEl.classList.add("bg-dark");
    loadingEl.classList.add("bg-opacity-50");
    loadingEl.classList.add("d-flex");
    loadingEl.classList.add("align-items-center");
    loadingEl.classList.add("justify-content-center");
    loadingEl.classList.add("text-center");
    loadingEl.innerHTML = `
        <span class="spinner-border text-primary" role="status"></span>
        <span class="text-gray-800 fs-6 fw-semibold mt-5">Loading...</span>
    `;
});