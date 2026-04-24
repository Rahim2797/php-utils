<?php

use App\Models\Admin;
use App\Models\User;
use Rahim2797\MagicTypes\Properties;

/**
 * @return Properties<User>|Properties<Admin>
 */
function getUnionProperties(): array
{
    return [];
}

$props = getUnionProperties();
$value = $props['name'];
$other = $props['<caret>'];
