## coursier-private-repo-boostrap

Specialized [coursier](https://get-coursier.io/) launcher that reads credentials from .m2/setting.xml and .npmrc.

The use case is distributing coursier tools on private repositories.

Instead of having them
* Install coursier
* Write their credentials to the right place
* Run `cs install ...`

You can use this to do all three in one step, assuming they already have
credentials in `.m2/settings.xml` or `.npmrc`.


### How to use


```bash
curl https://launcher-path/tbd | sh -s -- <cs-install-args>
```

for example
```bash
curl https://launcher-path/tbd | sh -s -- -r https://somecompany.com/repo my-special-app cs
```

this will install cs itself and my-special-app and add the somecompany.com repo to
the list of repositories. See [coursier install docs](https://get-coursier.io/docs/cli-install)
for more options.

### Options

* `--try-m2  <bool>`

    Default: true
    
    Look for credentials in .m2/settings.xml
    
    
*  --try-npm  <bool>

    Default: false
    
    Look for credentials in .npmrc
    
*  --save-credentials  <bool>

    Default: false
    
    Write the credentials to coursier's credentials.properties;
     otherwise we just forward them along with `--credentials`
    
