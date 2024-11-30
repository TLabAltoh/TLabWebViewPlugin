async function acceptAndContinue(temporary) {
    try {
        await document.addCertException(temporary);
        location.reload();
    } catch (error) {
        console.error("Unexpected error: " + error);
    }
};
acceptAndContinue(true);