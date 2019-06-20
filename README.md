## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Date and Time](https://www.joda.org/joda-time/userguide.html) - Date and Time Library

## Some Design Decisions

The problem at hand posses a few thoughts:

1. How do we handle failed payments? Do we introduce a retry mechanism, will this also then introduce a queuing mechanism?
2. Concurrency - should we call the payment provider in a billing attempt concurrently, in order to process the invoices faster?
3. If we go with concurrency we need to think about how frequenty we are hitting the payment provider, so as not to overwhelm the service with requests.
4. Batching the processing - What is a good rate to hit the payment provider, this will inform how we batch the processing.

With all these thoughts, here is what I decided to go with as an approach:

1. Introduced a retry mechanism. This called for queuing the requests, and how often we retry. So I added a payment attempt date on the invoice, so as to allow for retry frequency to be managed.
I also went with the option of not using a cache or even a persisted cache to hold the entities for retry or the retry queue, and the reason being queues are pretty much LIFO, and so everytime
my retry process runs daily, I cannot use the power of the DB to fetch only the invoices I need to process, I would need to add an logic to check if the retry frequency
is met before processing the billing, as opposed to allowing the DB to do all the work for me. So instead of running through an entire queue of possibly 100000 invoices,
I only run through a queue of say 20000 invoices that are due for retry. This is why I allowed the DB to hold my retry queue instead. Of course this can still be heavily
optimised with more time to it.

2. I opted for concurrency, this allows us to process the invoices faster. With this I introduced batching the processing, so that we are not trying to bombard the payment provider
service with a ton of requests at the same time. So we are trying to be efficient, but also not trying to kill nd get a DOS from our payment provider.
This would obviously depend on how much of a load the provider can handle.

## Some Enhancements and Benefits

1. I think with the retry approach and possibly also storing each attempt and the reason for failure, we can easily pull some great reporting from this, that will either inform the efficiency and
effectiveness of our system, the efficiency and effectiveness of our payment provider and also how our customers are fairing in terms of payments. Some examples would be:
    - Are we effectively processing the payments and also efficiently processing the payments in such a way that does not overload our systems. And if we are overloading our
    systems or servers, how can we improve this, how can we improve our network stability. I think we are able to assess things like these from maintaining a DB queue for the 
    retries and maintaining retry attempts and the results of it.
    - Is our payment provider able to handle our payment needs, at the speed and capacity we need, is their link stable, etc. Should we start shopping for a new payment provider?
    - Are our customers paying on time, if not why? Which customers are not paying on time, how are these customers performing in terms of payments over time? Together with these
    records we can look at their expenditure as well and possibly better advice or infer a reason why payments are delayed and maybe better advice on spending to make sure we stay
    in business as well. :D

2. I could have catered for a lot more error cases, had I had time and should this have been a real live system, some of those scenarios would have been viable, for one, timeout errors,
insufficient funds errors, and many more cases.

## Some Roadblocks

1. This not a roadblock per say, but I had to learn kotlin, which was easier because I already knew Java, but things are still slight different. Majorly the OO concepts are still the same
but how certain things are handled were different, so I produced what I could in the space of time that I had, also considering that I had to do this in the evenings after work.
