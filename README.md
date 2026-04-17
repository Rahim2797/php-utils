# PHP Utilities

PhpStorm companion plugin for practical PHP and Laravel development helpers, starting with smart support for custom `Properties<T>` PHPDoc types.

![Build](https://github.com/Rahim2797/php-utils/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Features

<!-- Plugin description -->

Adds conservative PhpStorm/Laravel workflow enhancements, with `Properties<T>` as the first implemented magic type.

`Properties<T>` is treated as an array-like structure whose valid keys are the properties of class `T`.

Features:
- Autocomplete for array keys like `$props['username']`
- Go to declaration from an array key to the corresponding class property
- Type inference for accessed values, based on the target property type
- Documentation rendering for `Properties<T>` PHPDoc usages
- Contradiction diagnostics when `@return Properties<T>` conflicts with a non-array native return type
- Support for `Properties<T>` in common local contexts such as variable annotations, returns, parameters, and array literals

Planned direction:
- Additional magic types inspired by utility types such as `Pick` and `Omit`
- Typed data-bag helpers for object-like shaped payloads
- Smart refactor helpers for array/object shape transformations

Example:

```php
/** @var Properties<User> $props */
$props = [];

$username = $props['username'];
$isActive = $props['is_activated']; 
```

With `Properties<User>`, the plugin resolves valid keys from `User` fields and makes array access behave like a typed property map.

This is especially useful for codebases that model validated or projected object data as associative arrays while still wanting strong editor assistance.

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "php-utils"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/Rahim2797/php-utils/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
