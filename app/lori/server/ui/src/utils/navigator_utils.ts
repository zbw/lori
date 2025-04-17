export default {
    copyToClipboard(textToCopy: string | undefined): void {
        if (textToCopy == undefined){
            return;
        }
        navigator.clipboard.writeText(textToCopy);
    },
};
