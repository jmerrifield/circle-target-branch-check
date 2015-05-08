# circle-target-branch-check

Simple service to attach a Github status message to Pull Requests, with the current CircleCI status of the PR's target branch.

When a CircleCI build completes, this looks for any open Pull Requests based off the branch that was built, and posts a status to each one informing the PR author whether their target branch is red or green.

When a Pull Request is opened or pushed to, this asks CircleCI for the latest build for the PR's base branch, and posts a status to the PR.

## Setup

### Deploy

1. Clone this repo and deploy (Heroku is easy).
1. Set the `GH_TOKEN` environment variable to a Github access token with `repo` scope (or `public_repo` for public repositories).
1. Set the `CIRCLE_TOKEN` environment variable to a [CircleCI API Token](https://circleci.com/account/api).

### Github webhook

* Create a Github webhook for the repo you wish to use. 
* Set the Payload URL to the deployed URL of this app, plus `/payload/github` (e.g. `http://myservice.example.com/payload/github`).
* Set the webhook to send only events of type 'Pull Request'.

### Circle webhook

Configure a [Circle notification](https://circleci.com/docs/configuration#notify) pointing at the deployed URL of this app, plus `/payload/circle`. e.g:

```yaml
notify:
  webhooks:
    - url: https://myservice.example.com/payload/circle
```

# Developing

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2015 FIXME
