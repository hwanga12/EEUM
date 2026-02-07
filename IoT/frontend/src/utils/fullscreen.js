/**
 * Fullscreen API Utility
 * Provides cross-browser support for entering and exiting fullscreen mode.
 */

const getRequestFullscreen = (element) => {
    return (
        element.requestFullscreen ||
        element.webkitRequestFullscreen ||
        element.mozRequestFullScreen ||
        element.msRequestFullscreen
    );
};

const getExitFullscreen = () => {
    const doc = window.document;
    return (
        doc.exitFullscreen ||
        doc.webkitExitFullscreen ||
        doc.mozCancelFullScreen ||
        doc.msExitFullscreen
    );
};

export const isFullscreen = () => {
    const doc = window.document;
    return !!(
        doc.fullscreenElement ||
        doc.webkitFullscreenElement ||
        doc.mozFullScreenElement ||
        doc.msFullscreenElement
    );
};

export const requestFullscreen = async (element = document.documentElement) => {
    const request = getRequestFullscreen(element);
    if (request) {
        try {
            await request.call(element);
            return true;
        } catch (err) {
            console.error(`Error attempting to enable full-screen mode: ${err.message} (${err.name})`);
            return false;
        }
    }
    return false;
};

export const exitFullscreen = async () => {
    if (!isFullscreen()) return true;

    const exit = getExitFullscreen();
    if (exit) {
        try {
            await exit.call(window.document);
            return true;
        } catch (err) {
            console.error(`Error attempting to exit full-screen mode: ${err.message} (${err.name})`);
            return false;
        }
    }
    return false;
};

export const toggleFullscreen = async (element = document.documentElement) => {
    if (isFullscreen()) {
        return await exitFullscreen();
    } else {
        return await requestFullscreen(element);
    }
};
