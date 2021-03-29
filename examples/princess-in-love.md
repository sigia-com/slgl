# "Princess in love" contract in SLGL

## Story

> Once upon a time, there lived a beautiful princess called Julia. She was kind, gentle, and praised by everyone for her beauty.
>
> One beautiful day, two princes, Drago and Jim, talk about the beauty of Princess Julia. Suddenly, Drago brags that he can win the princess’s hand. Prince Jim can’t believe in Drago’s big words. Drago offers a bet.

## Contract description

This bet between Drago and Jim can be represented with a smart contract with following rules:
1. If anyone can `deliver a document signed by Princess Julia that proofs that Julia loves him` and `agreed time limit is not over yet` 
   then `Drago wins the bet` 
2. If `Drago hasn't won yet` and `agreed time limit is over` then `Jim wins the bet`

## Contract model in SLGL

This smart contract can be represented in SLGL as a single node.

That SLGL node would have two anchors to represent results of the bet:
- anchor `#drago_wins` - if any node is linked to this anchor then it means that Drago has won the bet
- anchor `#jim_wins` - if any node is linked to this anchor then is means that Jim has won the bet

Additionally, to ensure smart contract rules we must define permissions that will allow to link to these anchors only if specyfic requirements are met:
- for anchor `#drago_wins` we will add permission that node can be linked to that anchor only if:
  - document is being linked
  - that document has following content: `My heart is and always will be yours, Drago.`
  - that document is signed by Princess Julia's certificate (certificate with `CN=Princess Julia`)
  - current time is before `bet end time`
- for anchor `#jim_wins` we will add permission that allows to link node to that anchor only if:
  - current time is after `bet end time`
  - anchor `#drago_wins` doesn't have any nodes linked to it
  
## SLGL request for creating smart contract

To create node that will represent this smart contract you can execute following command:

```bash
curl <API_URL> -u <USERNAME>:<API_KEY> -H 'Content-Type: application/json' \
-d '{
  "requests" : [ {
    "node": {
      "@type" : {
        "anchors" : [ {
          "id" : "#drago_wins",
          "max_size" : 1
        }, {
          "id" : "#jim_wins",
          "max_size" : 1
        } ],
        "permissions" : [ {
          "allow" : [ {
            "action" : "link_to_anchor",
            "anchor" : "#drago_wins"
          } ],
          "require" : {
            "$source_node.$file.text": "My heart is and always will be yours, Drago.",
            "$source_node.$file.document_signatures" : {
              "op" : "at_least_one_meets_requirements",
              "as" : "signature",
              "value" : [ {
                "signature.certificate.subject.CN" : "Princess Julia"
              } ]
            },
            "$created" : {
              "op" : "before",
              "value" : "2077-01-01T00:00:00.000Z"
            }
          }
        }, {
          "allow" : [ {
            "action" : "link_to_anchor",
            "anchor" : "#jim_wins"
          } ],
          "require" : {
            "$created" : {
              "op" : "after",
              "value" : "2077-01-01T00:00:00.000Z"
            },
            "$target_node.#drago_wins.$length": 0
          }
        } ]
      }
    }
  } ]
}' > princess_in_love_response.json
```

Notes:
- agreed time limit for bet is set to `year 2077` - you may want to change this (for example to some date in the past) to test scenario where Jim wins
- response is saved to file `princess_in_love_response.json` - that response will be needed to make other requests
- other requests use command line tool `jq` to manipulate JSON data - this tool can be downloaded [here](https://stedolan.github.io/jq/download/) 

## SLGL request for Drago winning

For Drago to win you must download [love letter signed by Julia](files/princess_julia_love_letter_signed.pdf) and execute following request: 

```bash
curl <API_URL> -u <USERNAME>:<API_KEY> -H 'Content-Type: application/json' \
-d @- <<EOF
{
  "requests" : [ {
    "node" : {
      "@file" : "`base64 < princess_julia_love_letter_signed.pdf | tr -d '[:space:]'`"
    }
  }, {
    "link" : {
      "source_node" : 0,
      "target_node" : `jq '.responses[0].node."@id"' < princess_in_love_response.json`,
      "target_anchor" : "#drago_wins"
    }
  } ],
  "existing_nodes" : {
    "state": `jq '[.responses[].node? | select(.) | {(."@id"): ."@state"}] | add' < princess_in_love_response.json`
  }
}
EOF
```

## SLGL request for Jim winning

For Jim to win you must wait for agreed time limit to pass (or change the request and set it to `+0 min`) and then execute following request:

```bash
curl <API_URL> -u <USERNAME>:<API_KEY> -H 'Content-Type: application/json' \
-d @- <<EOF
{
  "requests" : [ {
    "node" : {}
  }, {
    "link" : {
      "source_node" : 0,
      "target_node" : `jq '.responses[0].node."@id"' < princess_in_love_response.json`,
      "target_anchor" : "#jim_wins"
    }
  } ],
  "existing_nodes" : {
    "state": `jq '[.responses[].node? | select(.) | {(."@id"): ."@state"}] | add' < princess_in_love_response.json`
  }
}
EOF
```

## Examples of other SLGL request that are not valid

All these request will result in `permission_denied` error because they violate contract rules:

Request | Why it is not valid
--------|--------------------
upload [document not signed by Princess Julia](files/princess_julia_love_letter.pdf) to anchor `#drago_wins` | will not work because document must be signed by Princess Julia
upload [document with love rejection text `My heart is not and never will be yours, Drago.` signed by Princess Julia](files/princess_julia_rejection_letter_signed.pdf) to anchor `#drago_wins` | will not work because document must contain text `My heart is and always will be yours, Drago.` 
upload [correct document](files/princess_julia_love_letter_signed.pdf) but after agreed time limit has passed to anchor `#drago_wins` | will not work because links to anchor `#drago_wins` can only be done before agreed time has passed
link any node to anchor `#jim_wins` before agreed time limit has passed | will not work because links to anchor `#jim_wins` can only be done after agreed time has passed
link any node to anchor `#jim_wins` after Drago has already won | will not work because links to anchor `#jim_wins` can only be done where there are no links to anchor `#drago_wins`
