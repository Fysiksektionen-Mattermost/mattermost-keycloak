This directory contains the same file as tls/local-ca.crt, but is duplicated here to help defend against accidental overwrites of the tls directory.

> Note:
> ../tls/local-ca.crt is marked with
> ```bash
> git update-index --assume-unchanged ...
> ```
> so changes to it will not automatically be committed.