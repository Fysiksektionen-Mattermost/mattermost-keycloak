This directory contains the same file as tls/fullchain.pem, but is duplicated here to help defend against accidental overwrites of the tls directory.

> Note:
> ../tls/fullchain.pem is marked with
> ```bash
> git update-index --assume-unchanged ...
> ```
> so changes to it will not automatically be committed.